/*-
 * ---license-start
 * EU-Federation-Gateway-Service / efgs-federation-gateway
 * ---
 * Copyright (C) 2020 T-Systems International GmbH and all other contributors
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

import eu.interop.federationgateway.filter.CertificateAuthentificationRequired;
import eu.interop.federationgateway.model.AuditEntry;
import eu.interop.federationgateway.service.DiagnosisKeyEntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/diagnosiskeys")
@Validated
public class AuditController {

  private static final String AUDIT_DOWNLOAD_ROUTE = "/audit/download/{batchTag}";

  private final DiagnosisKeyEntityService diagnosisKeyEntityService;

  /**
   * This endpoint returns audit information for the interop gateway to inspect the exchanged data.
   *
   * @param batchTag {@link String}
   * @return List of {@link AuditEntry} with country codes and corresponding audit information.
   */
  @Operation(
    summary = "Gets audit information about the selected batchtag.",
    tags = {"Diagnosis Keys Exchange Interface", "Audit"},
    parameters = {
      @Parameter(
        name = "batchTag",
        in = ParameterIn.PATH,
        required = true,
        description = "batchTag with which the database is searched.",
        example = "20200730-1"
      ),
      @Parameter(
        name = "Accept",
        in = ParameterIn.HEADER,
        required = true,
        example = MediaType.APPLICATION_JSON_VALUE
      )
    },
    responses = {
      @ApiResponse(responseCode = "200", description = "OK. Returns the audit information to the selected batch."),
      @ApiResponse(responseCode = "400", description = "Invalid BatchTag used.", content = @Content),
      @ApiResponse(responseCode = "403",
        description = "Forbidden call in cause of missing or invalid client certificate.", content = @Content),
      @ApiResponse(responseCode = "404", description = "BatchTag not found or no data exists.", content = @Content),
      @ApiResponse(responseCode = "406", description = "Data format or content is not valid.", content = @Content)}
  )
  @GetMapping(value = AUDIT_DOWNLOAD_ROUTE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @CertificateAuthentificationRequired
  public ResponseEntity<List<AuditEntry>> getAuditInformation(
    @PathVariable("batchTag") String batchTag
  ) {

    List<AuditEntry> auditResponse
      = diagnosisKeyEntityService.getAllDiagnosisKeyEntityByBatchTag(batchTag);

    if (auditResponse.isEmpty()) {
      log.error("BatchTag Could not found\", batchTag=\"{}", batchTag);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Batchtag not found");
    }
    log.info("Requested Audit Information\", batchTag=\"{}", batchTag);
    return new ResponseEntity<>(auditResponse, HttpStatus.OK);
  }

}
