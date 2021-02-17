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
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.entity.FormatInformation;
import eu.interop.federationgateway.repository.DiagnosisKeyBatchRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import eu.interop.federationgateway.utils.EfgsMdc;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * A service that holds methods for DiagnosisKeyBatchService.
 * These methods are separated in this class to enable transaction handling via Spring Boot proxy.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class TransactionalDiagnosisKeyBatchService {

  private final EfgsProperties properties;
  private final DiagnosisKeyEntityRepository diagnosisKeyEntityRepository;
  private final DiagnosisKeyBatchRepository diagnosisKeyBatchRepository;
  private final CallbackService callbackService;

  /**
   * Creates a new Batch Entity and sets batchTag to all contained diagnosiskeys.
   *
   * @return true if a batch was created or false if not.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public boolean createNextBatch() {

    List<String> uploaderBatchTags = collectUploaderBatchTags();

    if (uploaderBatchTags.isEmpty()) {
      log.info("Successfully finished the document batching process - no more unprocessed diagnosis keys left");
      return false;
    }

    DiagnosisKeyBatchEntity newBatchEntity = createNextBatchEntityAndLinkPredecessor();

    int updatedRows = diagnosisKeyEntityRepository.setBatchTagByUploaderBatchTag(
      uploaderBatchTags, newBatchEntity.getBatchName());

    callbackService.notifyAllCountriesForNewBatchTag(newBatchEntity);


    EfgsMdc.put("batchTag", newBatchEntity.getBatchName());
    EfgsMdc.put("diagnosisKeyCount", updatedRows);
    log.info("Batch created");
    EfgsMdc.remove("diagnosisKeyCount");
    EfgsMdc.remove("batchTag");

    return true;
  }

  /**
   * Creates a new instance of DiagnosisKeyBatchEntity und generates a batchTag for it.
   * Also the preceding batch entity will be linked to the newly created batch entity.
   *
   * @return the created DiagnosisKeyBatchEntity.
   */
  @Transactional(Transactional.TxType.MANDATORY)
  DiagnosisKeyBatchEntity createNextBatchEntityAndLinkPredecessor() {
    DiagnosisKeyBatchEntity newBatchEntity = new DiagnosisKeyBatchEntity();
    newBatchEntity.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));

    String formattedDate = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    Optional<DiagnosisKeyBatchEntity> lastBatchEntity = diagnosisKeyBatchRepository.findTopByOrderByCreatedAtDesc();

    String newBatchTag;

    // for today a batch already exists so generate the upcoming batchtag
    if (lastBatchEntity.isPresent() && isBatchFromToday(lastBatchEntity.get())) {
      newBatchTag = formattedDate + "-" + (getSequenceNumberFromBatchTag(lastBatchEntity.get().getBatchName()) + 1);

      lastBatchEntity.get().setBatchLink(newBatchTag);
      diagnosisKeyBatchRepository.save(lastBatchEntity.get());
    } else { // otherwise create the first batch for today
      newBatchTag = formattedDate + "-1";
    }

    newBatchEntity.setBatchName(newBatchTag);
    return diagnosisKeyBatchRepository.save(newBatchEntity);
  }

  /**
   * Queries the database for unbatched uploaded keys until the maximum batch size is reached.
   *
   * @return a list of uploader batch tags that need to be put into one batch.
   */
  @Transactional(Transactional.TxType.MANDATORY)
  List<String> collectUploaderBatchTags() {
    List<String> uploaderBatchTags = new ArrayList<>();
    int newBatchSize = 0;
    FormatInformation batchFormat = null;

    while (true) {
      Optional<DiagnosisKeyEntity> unbatchedDiagnosisKey = uploaderBatchTags.isEmpty()
        ? diagnosisKeyEntityRepository.findFirstByBatchTagIsNull()
        : diagnosisKeyEntityRepository.findFirstByBatchTagIsNullAndUploaderBatchTagIsNotIn(uploaderBatchTags);

      if (unbatchedDiagnosisKey.isEmpty()) {
        // no more unprocessed keys
        break;
      }

      if (batchFormat == null) {
        batchFormat = unbatchedDiagnosisKey.get().getFormat();
      } else {
        // stop batch tag collecting when next upload batch has different format
        if (!batchFormat.equals(unbatchedDiagnosisKey.get().getFormat())) {
          break;
        }
      }

      String uploaderBatchTag = unbatchedDiagnosisKey.get().getUploader().getBatchTag();
      int uploadBatchSize = diagnosisKeyEntityRepository.countAllByUploader_BatchTag(uploaderBatchTag);

      if (newBatchSize + uploadBatchSize <= properties.getBatching().getDoclimit()) {
        newBatchSize += uploadBatchSize;
        uploaderBatchTags.add(uploaderBatchTag);
      } else {
        break;
      }
    }

    return uploaderBatchTags;
  }

  private boolean isBatchFromToday(DiagnosisKeyBatchEntity lastEntry) {
    return lastEntry.getCreatedAt().toInstant().truncatedTo(ChronoUnit.DAYS).equals(
      Instant.now().truncatedTo(ChronoUnit.DAYS));
  }

  private int getSequenceNumberFromBatchTag(String batchTag) {
    String lastBatchSequence = batchTag.substring(batchTag.indexOf("-") + 1);
    return Integer.parseInt(lastBatchSequence);
  }
}
