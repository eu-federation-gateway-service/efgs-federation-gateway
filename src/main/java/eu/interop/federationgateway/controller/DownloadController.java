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
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.filter.CertificateAuthentificationFilter;
import eu.interop.federationgateway.filter.CertificateAuthentificationRequired;
import eu.interop.federationgateway.mapper.DiagnosisKeyMapper;
import eu.interop.federationgateway.model.EfgsProto;
import eu.interop.federationgateway.service.DiagnosisKeyBatchService;
import eu.interop.federationgateway.service.DiagnosisKeyEntityService;
import eu.interop.federationgateway.utils.EfgsMdc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
public class DownloadController {

  private static final String DOWNLOAD_ROUTE = "/download/{date}";
  private static final String BATCHTAG_HEADER = "batchTag";
  private static final String NEXT_BATCHTAG_HEADER = "nextBatchTag";
  private static final String MDC_PROP_BATCHTAG = "batchTag";
  private static final String MDC_PROP_NUMKEYS = "numKeys";

  private final EfgsProperties properties;

  private final DiagnosisKeyBatchService diagnosisKeyBatchService;

  private final DiagnosisKeyEntityService diagnosisKeyService;

  private final DiagnosisKeyMapper diagnosisKeyMapper;

  /**
   * This endpoint enabled the download of diagnosis keys.
   *
   * @param date A {@link String} containing an ISO-8601 date descriptor.
   * @param batchTag A {@link String} containing batchTag.
   * @param downloaderCountry A {@link String} containing downloader country.
   * @return Key-Value map with country codes and corresponding audit information.
   */
  @Operation(
    summary = "Downloads diagnosis keys dataset by date.",
    description = "Downloads the latest data by date. The date indicates the start point for "
      + "query. Means the last 5 Days are currentDate-5",
    tags = {"Diagnosis Keys Exchange Interface", "Download"},
    parameters = {
      @Parameter(
        name = "date",
        in = ParameterIn.PATH,
        required = true,
        description = "Date from where the query should start until today.",
        example = "2020-07-31"
      ),
      @Parameter(
        name = "batchTag",
        in = ParameterIn.HEADER,
        description = "Optional Tag to submit the last received batchTag of the day.",
        example = "20200731-1"
      )
    },
    responses = {
      @ApiResponse(responseCode = "200", description = "OK.", headers = {
        @Header(name = BATCHTAG_HEADER, required = true, description = "Tag of the batch."),
        @Header(name = NEXT_BATCHTAG_HEADER, required = true,
          description = "Tag of the next available batch of the day. Has the value \"null\" if no further BatchTag"
            + " exists for requested date")},
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE + "+v1.0",
          examples = @ExampleObject("diagnosisKeyBatch")
        )),
      @ApiResponse(responseCode = "400", description = "Invalid or missing request header.", content = @Content),
      @ApiResponse(responseCode = "403",
        description = "Forbidden call in cause of missing or invalid client certificate.", content = @Content),
      @ApiResponse(responseCode = "404", description = "BatchTag not found or no data exists.", content = @Content),
      @ApiResponse(responseCode = "406", description = "Data format or content is not valid.", content = @Content),
      @ApiResponse(responseCode = "410", description = "Date for download expired. Date does not more exists.",
        content = @Content),
    })
  @GetMapping(value = DOWNLOAD_ROUTE,
    produces = {"application/protobuf", "application/json"}
  )
  @CertificateAuthentificationRequired
  public ResponseEntity<EfgsProto.DiagnosisKeyBatch> downloadDiagnosisKeys(
    @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
    @RequestHeader(name = BATCHTAG_HEADER, required = false) String batchTag,
    @RequestAttribute(CertificateAuthentificationFilter.REQUEST_PROP_COUNTRY) String downloaderCountry
  ) {

    EfgsMdc.put("requestedDate", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
    EfgsMdc.put(MDC_PROP_BATCHTAG, batchTag);

    ZonedDateTime thresholdDate = ZonedDateTime.now(ZoneOffset.UTC)
      .minusDays(properties.getDownloadSettings().getMaxAgeInDays());

    if (date.isBefore(thresholdDate.toLocalDate())) {
      log.info("Requested date is too old");
      throw new ResponseStatusException(HttpStatus.GONE, "Requested date is too old!");
    }

    if (batchTag == null) {
      batchTag = diagnosisKeyBatchService.getFirstBatchTagOfTheDay(date);

      if (batchTag == null) {
        log.info("Could not find any batches for given date");
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find any batches for given date");
      }
    }

    Optional<DiagnosisKeyBatchEntity> batchEntity = diagnosisKeyBatchService.getBatchEntity(batchTag);

    if (batchEntity.isEmpty()) {
      log.info("Could not find batch with given batchTag");
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find batch with given BatchTag");
    }

    Instant batchDate = batchEntity.get().getCreatedAt().toInstant().truncatedTo(ChronoUnit.DAYS);
    Instant dateAsInstant = date.atStartOfDay(ZoneOffset.UTC).toInstant();

    if (!batchDate.equals(dateAsInstant)) {
      log.info("Given date does not match the requested batchTag");
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Given date does not match the requested batch");
    }

    List<DiagnosisKeyEntity> entities =
      diagnosisKeyService.getDiagnosisKeysBatchForCountry(batchTag, downloaderCountry);

    EfgsProto.DiagnosisKeyBatch protoBatch = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(diagnosisKeyMapper.entityToProto(entities))
      .build();
    
    EfgsMdc.put(MDC_PROP_NUMKEYS, protoBatch.getKeysCount());

    String nextBatchTag = "null";
    if (batchEntity.get().getBatchLink() != null) {
      nextBatchTag = batchEntity.get().getBatchLink();
    }

    log.info("Successful Batch Download");

    return ResponseEntity
      .ok()
      .header(BATCHTAG_HEADER, batchTag)
      .header(NEXT_BATCHTAG_HEADER, nextBatchTag)
      .body(protoBatch);
  }

}
