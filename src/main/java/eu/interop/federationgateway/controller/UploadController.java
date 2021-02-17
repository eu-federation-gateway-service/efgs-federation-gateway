/*-
 * ---license-start
 * EU-Federation-Gateway-Service / efgs-federation-gateway
 * ---
 * Copyright (C) 2020 - 2021 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package eu.interop.federationgateway.controller;

import eu.interop.federationgateway.batchsigning.BatchSignatureVerifier;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.filter.CertificateAuthentificationFilter;
import eu.interop.federationgateway.filter.CertificateAuthentificationRequired;
import eu.interop.federationgateway.mapper.DiagnosisKeyMapper;
import eu.interop.federationgateway.model.EfgsProto;
import eu.interop.federationgateway.service.DiagnosisKeyEntityService;
import eu.interop.federationgateway.utils.EfgsMdc;
import eu.interop.federationgateway.validator.DiagnosisKeyBatchConstraint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/diagnosiskeys")
@Validated
public class UploadController {

  private static final String UPLOAD_ROUTE = "/upload";

  private final BatchSignatureVerifier signatureVerifier;

  private final DiagnosisKeyMapper diagnosisKeyMapper;

  private final EfgsProperties properties;

  private final DiagnosisKeyEntityService diagnosisKeyEntityService;

  /**
   * This endpoint enables the upload of diagnosis keys.
   * @param batchTag A {@link String} containing batchTag.
   * @param batchSignature A {@link String} containing batchSignature.
   * @param contentType A {@link String} containing Content-Type.
   * @param body The Requestbody with payload.
   * @param uploaderCountry A {@link String} containing uploaderCountry.
   * @param uploaderCertThumbprint A {@link String} containing uploaderCertThumbprint.
   * @return The successfull batch upload batchTag.
   * @throws DiagnosisKeyEntityService.DiagnosisKeyInsertException will be thrown if an error occurred during insertion.
   */
  @Operation(
    summary = "Uploads diagnosis key datasets.",
    description = "Uploads the given batch to the server. Uploader Information is given by the client certificate.",
    tags = {"Diagnosis Keys Exchange Interface", "Upload"},
    parameters = {
      @Parameter(
        name = "batchTag",
        in = ParameterIn.HEADER,
        required = true,
        description = "Required Tag to tag the send batch (must be unique).",
        example = "20200731-1, or a hash"
      ),
      @Parameter(
        name = "batchSignature",
        in = ParameterIn.HEADER,
        required = true,
        description = "PKC7 Payload signature in Base64 encoding.",
        example = "ABDBJ345231DJ122..."
      ),
      @Parameter(
        name = "Content-Type",
        in = ParameterIn.HEADER,
        required = true,
        example = "application/protobuf; version=1.0")
    },
    requestBody = @RequestBody(
      content = {
        @Content(mediaType = "application/protobuf; version=1.0"),
        @Content(mediaType = MediaType.APPLICATION_JSON_VALUE + "; version=1.0")
      },
      description = "Requestbody with payload. (limited)"
    ),
    responses = {
      @ApiResponse(responseCode = "201", description = "Database Entries created.", headers = {
        @Header(name = "batchTag", required = true, description = "Tag of the batch.")
      }, content = @Content),
      @ApiResponse(
        responseCode = "207",
        description = "Data partially added with warnings. More details in document.",
        headers = {
          @Header(name = "batchTag", required = true, description = "Tag of the batch.")
        },
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE + "+v1.0",
          examples = @ExampleObject("{\n  '201': [1,2,5,8,9],\n  '409': [3,4,6,7],\n  '500': [10]\n}"))
      ),
      @ApiResponse(responseCode = "400", description = "Signature not valid. Bad request.", content = @Content),
      @ApiResponse(responseCode = "403",
        description = "Forbidden call in cause of missing or invalid client certificate.", content = @Content),
      @ApiResponse(responseCode = "406", description = "Data format or content is not valid.", content = @Content),
      @ApiResponse(responseCode = "413", description = "Payload to large.", content = @Content),
      @ApiResponse(responseCode = "413", description = "Data already exist.", content = @Content),
      @ApiResponse(responseCode = "500", description = "Not able to write data. Retry please.", content = @Content),
    })
  @PostMapping(value = UPLOAD_ROUTE,
    consumes = {"application/protobuf", MediaType.APPLICATION_JSON_VALUE},
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  @CertificateAuthentificationRequired
  public ResponseEntity<String> uploadDiagnosisKeys(
    @RequestHeader(value = "batchTag") String batchTag,
    @RequestHeader(value = "batchSignature") String batchSignature,
    @RequestHeader(name = "Content-Type") String contentType,
    @org.springframework.web.bind.annotation.RequestBody @Parameter(hidden = true)
    @DiagnosisKeyBatchConstraint EfgsProto.DiagnosisKeyBatch body,
    @RequestAttribute(CertificateAuthentificationFilter.REQUEST_PROP_COUNTRY) String uploaderCountry,
    @RequestAttribute(CertificateAuthentificationFilter.REQUEST_PROP_THUMBPRINT) String uploaderCertThumbprint
  ) throws DiagnosisKeyEntityService.DiagnosisKeyInsertException {
    int maximumUploadBatchSize = properties.getUploadSettings().getMaximumUploadBatchSize();

    EfgsMdc.put("batchTag", batchTag);
    EfgsMdc.put("numKeys", body.getKeysCount());
    EfgsMdc.put("maxKeys", maximumUploadBatchSize);

    if (body.getKeysCount() > maximumUploadBatchSize) {
      log.error("too many diagnosis keys");
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Too many diagnosis keys");
    }

    if (diagnosisKeyEntityService.uploadBatchTagExists(batchTag)) {
      log.error("batchTag already exists");
      throw new ResponseStatusException(HttpStatus.CONFLICT, "BatchTag already exists.");
    }

    String signingCertThumbprint = signatureVerifier.checkBatchSignature(body, batchSignature);
    if (signingCertThumbprint == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature");
    }
    log.info("verified batch signature");

    if (body.getKeysList().stream().anyMatch(diagnosisKey -> !diagnosisKey.getOrigin().equals(uploaderCountry))) {
      log.error("invalid uploader country");
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "One or more keys are not originated from uploader country");
    }

    List<DiagnosisKeyEntity> entities = diagnosisKeyMapper.protoToEntity(
      body.getKeysList(),
      batchTag,
      batchSignature,
      uploaderCertThumbprint,
      signingCertThumbprint,
      uploaderCountry,
      MediaType.parseMediaType(contentType)
    );

    diagnosisKeyEntityService.saveDiagnosisKeyEntities(entities);
    log.info("successfull batch upload");

    return ResponseEntity
      .status(HttpStatus.CREATED)
      .header("batchTag", batchTag)
      .build();
  }

}
