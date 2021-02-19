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

import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import eu.interop.federationgateway.filter.CertificateAuthentificationRequired;
import eu.interop.federationgateway.model.AuditEntry;
import eu.interop.federationgateway.service.CertificateService;
import eu.interop.federationgateway.service.DiagnosisKeyBatchService;
import eu.interop.federationgateway.service.DiagnosisKeyEntityService;
import eu.interop.federationgateway.utils.EfgsMdc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
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

  private static final String AUDIT_DOWNLOAD_ROUTE = "/audit/download/{date}/{batchTag}";

  private final EfgsProperties properties;

  private final DiagnosisKeyEntityService diagnosisKeyEntityService;
  
  private final DiagnosisKeyBatchService diagnosisKeyBatchService;

  private final CertificateService certificateService;

  /**
   * This endpoint returns audit information for the interop gateway to inspect the exchanged data.
   *
   * @param date     A {@link String} containing an ISO-8601 date descriptor.
   * @param batchTag A {@link String} containing the batchTag
   * @return List of {@link AuditEntry} with country codes and corresponding audit information.
   */
  @Operation(
    summary = "Gets audit information about the selected batchtag on a specific date.",
    tags = {"Diagnosis Keys Exchange Interface", "Audit"},
    parameters = {
      @Parameter(
        name = "date",
        in = ParameterIn.PATH,
        required = true,
        description = "date with which the database is searched.",
        example = "2020-09-04"
      ),
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
      @ApiResponse(responseCode = "400", description = "Invalid or missing request header.", content = @Content),
      @ApiResponse(responseCode = "403",
        description = "Forbidden call in cause of missing or invalid client certificate.", content = @Content),
      @ApiResponse(responseCode = "404", description = "BatchTag not found or no data exists.", content = @Content),
      @ApiResponse(responseCode = "406", description = "Data format or content is not valid.", content = @Content),
      @ApiResponse(responseCode = "410", description = "Date for download expired. Date does not more exists.",
        content = @Content)}
  )
  @GetMapping(value = AUDIT_DOWNLOAD_ROUTE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @CertificateAuthentificationRequired
  public ResponseEntity<List<AuditEntry>> getAuditInformation(
    @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
    @PathVariable("batchTag") String batchTag
  ) {

    ZonedDateTime thresholdDate = ZonedDateTime.now(ZoneOffset.UTC)
      .minusDays(properties.getDownloadSettings().getMaxAgeInDays());

    if (date.isBefore(thresholdDate.toLocalDate())) {
      log.warn("Requested date is too old");
      throw new ResponseStatusException(HttpStatus.GONE, "Requested date is too old!");
    }

    List<AuditEntry> auditResponse
      = diagnosisKeyEntityService.getAllDiagnosisKeyEntityByBatchTag(batchTag);

    Optional<DiagnosisKeyBatchEntity> batchEntity = diagnosisKeyBatchService.getBatchEntity(batchTag);

    if (batchEntity.isEmpty()) {
      log.info("Could not find batch with given batchTag");
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find batch with given BatchTag");
    }

    Instant batchDate = batchEntity.get().getCreatedAt().toInstant().truncatedTo(ChronoUnit.DAYS);
    Instant dateAsInstant = date.atStartOfDay(ZoneOffset.UTC).toInstant();

    if (!batchDate.equals(dateAsInstant)) {
      log.info("Requested date does not match the BatchTag creation date");
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Given date does not match the requested batch");
    }

    auditResponse = certificateService.addOperatorSignatures(auditResponse);

    EfgsMdc.put("batchTag", batchTag);
    if (auditResponse.isEmpty()) {
      log.error("BatchTag Could not found");
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Batchtag not found");
    }
    log.info("Requested Audit Information");
    return new ResponseEntity<>(auditResponse, HttpStatus.OK);
  }

}
