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

package eu.interop.federationgateway.service;

import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.repository.DiagnosisKeyBatchRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A service to be split documents into batches to minimize download issues.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DiagnosisKeyBatchService {

  private final EfgsProperties properties;
  private final DiagnosisKeyEntityRepository diagnosisKeyEntityRepository;
  private final DiagnosisKeyBatchRepository diagnosisKeyBatchRepository;

  /**
   * scheduled service - bundles uploaded documents into batches.
   */
  @Scheduled(
    fixedDelayString = "${efgs.batching.timeinterval}"
  )
  @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 10000))
  @Transactional
  public void batchDocuments() {
    log.info("start the document batching process");

    while (true) {
      // get the first key where the main batch tag is null
      Optional<DiagnosisKeyEntity> diagnosisKey = diagnosisKeyEntityRepository.findFirstByBatchTagIsNull();
      if (diagnosisKey.isEmpty()) {
        log.info("successfully finish the document batching process - no more unprocessed diagnosis keys left");
        break;
      }
      // find the last batch entry
      Optional<DiagnosisKeyBatchEntity> lastEntry = diagnosisKeyBatchRepository.findTopByOrderByCreatedAtDesc();
      // save batches
      String newBatchName = saveBatches(lastEntry);

      List<DiagnosisKeyEntity> diagnosisKeyCollector = new ArrayList<>();
      while (true) {
        // find one
        diagnosisKey = diagnosisKeyEntityRepository.findFirstByBatchTagIsNull();
        if (diagnosisKey.isEmpty()) {
          break;
        }

        List<DiagnosisKeyEntity> diagnosisKeyEntitys =
          findAllKeysByBatchTag(diagnosisKey.get().getUploader().getBatchTag());

        if (checkFormat(diagnosisKeyEntitys, diagnosisKey)) {
          return;
        }

        if (checkUploaderLimitation(diagnosisKeyEntitys, diagnosisKey.get().getUploader().getBatchTag())) {
          return;
        }

        diagnosisKeyCollector.addAll(diagnosisKeyEntitys);

        if (checkBatchSize(diagnosisKeyCollector)) {
          break;
        }

        // update keys
        updateDiagnosisKeys(diagnosisKeyEntitys, newBatchName);
      }
      log.info("Batch process finished\", batchCount=\"{}", diagnosisKeyCollector.size());
    }
    //TODO integrate into call back use case
  }

  @Recover
  public String recover(Throwable t) {
    log.info("DiagnosisKeyBatchService.recover");
    return "error class :: " + t.getClass().getName();
  }

  private List<DiagnosisKeyEntity> findAllKeysByBatchTag(String uploaderBatchTag) {
    // find all keys by the uploader batch tag
    List<DiagnosisKeyEntity> diagnosisKeyEntitys
      = diagnosisKeyEntityRepository.findByBatchTagIsNullAndUploader_BatchTag(uploaderBatchTag);
    log.info("found {} diagnosis keys with uploader tag {} for batching process",
      diagnosisKeyEntitys.size(), uploaderBatchTag);
    return diagnosisKeyEntitys;
  }

  private boolean checkBatchSize(List<DiagnosisKeyEntity> diagnosisKeyCollector) {
    if (diagnosisKeyCollector.size() > properties.getBatching().getDoclimit()) {
      log.info("Continue the batching process to next batch while {} "
          + "keys collected and the limit: {} of keys is reached",
        diagnosisKeyCollector.size(), properties.getBatching().getDoclimit());
      return true;
    }
    return false;
  }

  private boolean checkUploaderLimitation(List<DiagnosisKeyEntity> diagnosisKeyEntitys, String uploaderBatchTag) {
    if (diagnosisKeyEntitys.size() > properties.getBatching().getDoclimit()) {
      log.error("Stop batching process, while try to batch {} keys from one uploader: {}, "
          + "but the configured batching limit of keys is {}",
        diagnosisKeyEntitys.size(), uploaderBatchTag, properties.getBatching().getDoclimit());
      return true;
    }
    return false;
  }

  private boolean checkFormat(List<DiagnosisKeyEntity> diagnosisKeyEntitys, Optional<DiagnosisKeyEntity> diagnosisKey) {

    if (diagnosisKeyEntitys.stream()
      .map(DiagnosisKeyEntity::getFormat)
      .filter(Predicate.not(diagnosisKey.get().getFormat()::equals))
      .findAny()
      .isPresent()) {
      log.error("Stop batching process, while try to batch {} keys, but the keys have different format versions\""
        + ", format=\"{}", diagnosisKeyEntitys.size(), diagnosisKey.get().getFormat());
      return true;
    }
    return false;
  }

  private void updateDiagnosisKeys(List<DiagnosisKeyEntity> diagnosisKeyEntitys, String newBatchName) {
    diagnosisKeyEntitys.forEach(key -> key.setBatchTag(newBatchName));
    diagnosisKeyEntityRepository.saveAll(diagnosisKeyEntitys);
    log.info("Batch created\", batchTag=\"{}\", diagnosisKeysCount=\"{}",
      newBatchName, diagnosisKeyEntitys.size());
  }

  private String saveBatches(Optional<DiagnosisKeyBatchEntity> lastEntry) {
    ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    String formattedDate = currentDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    var batchEntity = new DiagnosisKeyBatchEntity();

    if (lastEntry.isPresent() && isBatchFromCurrentDay(lastEntry, currentDateTime)) {
      batchEntity.setBatchName(formattedDate + "-" + getSequenceFromPreviousBatchAndCountOn(lastEntry));
      lastEntry.get().setBatchLink(batchEntity.getBatchName());
      // update the last batch by link to the new batch
      diagnosisKeyBatchRepository.save(lastEntry.get());
      log.info("successfully updated the linked batch entity");
    } else {
      batchEntity.setBatchName(formattedDate + "-1");
    }
    batchEntity.setBatchLink(null);
    batchEntity.setCreatedAt(currentDateTime);
    diagnosisKeyBatchRepository.save(batchEntity);
    log.info("successfully save the new diagnosis key batch entity");
    return batchEntity.getBatchName();
  }

  private int getSequenceFromPreviousBatchAndCountOn(Optional<DiagnosisKeyBatchEntity> lastEntry)
    throws NumberFormatException {
    String lastBatchName = lastEntry.get().getBatchName();
    String lastBatchSequence = lastBatchName.substring(lastBatchName.indexOf("-") + 1);
    int sequence = Integer.parseInt(lastBatchSequence);
    return ++sequence;
  }

  private boolean isBatchFromCurrentDay(Optional<DiagnosisKeyBatchEntity> lastEntry, ZonedDateTime now) {
    return lastEntry.get().getCreatedAt().toLocalDate().isEqual(now.toLocalDate());
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

}
