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

package eu.interop.federationgateway.service;

import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import eu.interop.federationgateway.repository.DiagnosisKeyBatchRepository;
import eu.interop.federationgateway.utils.EfgsMdc;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A service to put uploaded diagnosiskeys into batches to minimize download issues.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DiagnosisKeyBatchService {

  private final EfgsProperties properties;
  private final DiagnosisKeyBatchRepository diagnosisKeyBatchRepository;
  private final TransactionalDiagnosisKeyBatchService transactionalDiagnosisKeyBatchService;

  /**
   * scheduled service - bundles uploaded documents into batches.
   */
  @Scheduled(
    fixedDelayString = "${efgs.batching.timeinterval}"
  )
  @SchedulerLock(name = "DiagnosisKeyBatchService_batchDocuments", lockAtLeastFor = "PT0S",
    lockAtMostFor = "${efgs.batching.locklimit}")
  public void batchDocuments() {
    log.info("Batch Process started");

    long startTime = System.currentTimeMillis();
    int batchCount = 0;

    boolean batchCreationResult;
    do {
      batchCreationResult = transactionalDiagnosisKeyBatchService.createNextBatch();

      long elapsedTime = System.currentTimeMillis() - startTime;
      if (elapsedTime > properties.getBatching().getTimelimit()) {
        EfgsMdc.put("batchTime", elapsedTime);
        log.info("Maximum time for one batching execution reached.");
        break;
      }

      batchCount += batchCreationResult ? 1 : 0;
    } while (batchCreationResult);

    EfgsMdc.put("batchCount", batchCount);
    log.info("Batch Process finished");


    EfgsMdc.remove("batchCount");
    EfgsMdc.remove("batchTime");
  }

  /**
   * Searches for a Batch Entity with given BatchTag.
   *
   * @param batchTag the BatchTag to search for.
   * @return an Optional containing the batch if exists.
   */
  public Optional<DiagnosisKeyBatchEntity> getBatchEntity(String batchTag) {
    return diagnosisKeyBatchRepository.findByBatchName(batchTag);
  }

  /**
   * Queries the database for the tag of the first batch of the given date.
   *
   * @param date the date for which the BatchTag should be searched.
   * @return the BatchTag or null if no batch exists.
   */
  public String getFirstBatchTagOfTheDay(LocalDate date) {
    ZonedDateTime begin = date.atStartOfDay(ZoneOffset.UTC);
    ZonedDateTime end = begin.plusDays(1).minusNanos(1);
    Optional<DiagnosisKeyBatchEntity> queryResult =
      diagnosisKeyBatchRepository.findFirstByCreatedAtIsBetweenOrderByCreatedAtAsc(begin, end);
    return queryResult.map(DiagnosisKeyBatchEntity::getBatchName).orElse(null);
  }

  /**
   * Deletes all DiagnosisKeyBatches which are older then timestamp.
   *
   * @param timestamp timestamp to check
   * @return the number of deleted rows.
   */
  public int deleteAllBefore(ZonedDateTime timestamp) {
    return diagnosisKeyBatchRepository.deleteByCreatedAtBefore(timestamp);
  }

}
