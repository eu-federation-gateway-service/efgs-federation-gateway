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

import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.model.AuditEntry;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import eu.interop.federationgateway.utils.EfgsMdc;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiagnosisKeyEntityService {

  @NonNull
  private final DiagnosisKeyEntityRepository diagnosisKeyEntityRepository;

  public boolean uploadBatchTagExists(String batchTag) {
    return diagnosisKeyEntityRepository.countAllByUploader_BatchTag(batchTag) != 0;
  }

  /**
   * Persists the specified entity of {@link DiagnosisKeyEntity} instances.
   *
   * @param diagnosisKeyEntity the verification app session entity
   */
  public void saveDiagnosisKeyEntity(DiagnosisKeyEntity diagnosisKeyEntity) {
    log.debug("Saving entity to db.");
    if (diagnosisKeyEntity.getCreatedAt() == null) {
      diagnosisKeyEntity.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
    }
    diagnosisKeyEntityRepository.save(diagnosisKeyEntity);
  }

  /**
   * Persists the specified entities of {@link DiagnosisKeyEntity} instances.
   *
   * @param diagnosisKeyEntities the diagnosis key entities
   * @throws DiagnosisKeyInsertException will be thrown if an error occurred during insertion.
   */
  @Transactional(
    rollbackOn = DiagnosisKeyInsertException.class,
    dontRollbackOn = DiagnosisKeyDuplicateInsertException.class)
  public void saveDiagnosisKeyEntities(
    List<DiagnosisKeyEntity> diagnosisKeyEntities
  ) throws DiagnosisKeyInsertException {
    HashMap<Integer, List<Integer>> resultMap = new HashMap<>();
    resultMap.put(201, new ArrayList<>());
    resultMap.put(409, new ArrayList<>());
    resultMap.put(500, new ArrayList<>());
    ZonedDateTime uploadTimestamp = ZonedDateTime.now(ZoneOffset.UTC);

    // Find and filter duplicate keys before trying to insert them into the database to avoid
    // a database transaction rollback. This way, the rest of the batch can be inserted into
    // the database (and reported as 201) even if there were some duplicate keys in the batch.
    List<String> batchPayloadHashes =
      diagnosisKeyEntities.stream().map((key) -> key.getPayloadHash()).collect(Collectors.toList());
    List<String> existingPayloadHashes = diagnosisKeyEntityRepository
      .getDiagnosisKeysByPayloadHashes(batchPayloadHashes)
      .stream()
      .map((k) -> k.getPayloadHash()).collect(Collectors.toList());

    for (int index = 0; index < diagnosisKeyEntities.size(); index++) {
      DiagnosisKeyEntity key = diagnosisKeyEntities.get(index);

      if (existingPayloadHashes.contains(key.getPayloadHash())) {
        // Key with the same payload hash already exists in database.
        // Report as 409 and skip inserting into database.
        resultMap.get(409).add(index);
        continue;
      } 

      key.setCreatedAt(uploadTimestamp);
      try {
        saveDiagnosisKeyEntity(key);
        resultMap.get(201).add(index);
        existingPayloadHashes.add(key.getPayloadHash());
      } catch (Exception e) {
        resultMap.get(500).add(index);
      }
    }
    if (!resultMap.get(500).isEmpty()) {
      // The INSERT of at least one key triggered an exception. We have to assume that the database
      // transaction will be rolled back and therefore need to report all previously inserted keys
      // as failed (500) and clear the list of 201.
      resultMap.get(500).addAll(resultMap.get(201));
      resultMap.get(201).clear();

      EfgsMdc.put("insertedKeyCount", resultMap.get(201).size());
      EfgsMdc.put("conflictKeysCount", resultMap.get(409).size());
      EfgsMdc.put("failedKeysCount", resultMap.get(500).size());

      log.error("error inserting keys");
      throw new DiagnosisKeyInsertException("Error during insertion of diagnosis keys!", resultMap);

    } else if (!resultMap.get(409).isEmpty()) {
      // Duplicate keys detected but no exception triggered during INSERT

      EfgsMdc.put("insertedKeyCount", resultMap.get(201).size());
      EfgsMdc.put("conflictKeysCount", resultMap.get(409).size());
      EfgsMdc.put("failedKeysCount", resultMap.get(500).size());

      log.error("error inserting keys");
      throw new DiagnosisKeyDuplicateInsertException("Error during insertion of diagnosis keys!", resultMap);
    }
  }

  /**
   * Gets all DiagnosisKeyEntitites as {@link DiagnosisKeyEntity} instances.
   *
   * @return all DiagnosisKeyEntitites
   */
  public List<DiagnosisKeyEntity> getAllDiagnosisKeyEntity() {
    log.info("Requested all DiagnosisKeyEntitites.");
    return diagnosisKeyEntityRepository.findAll();
  }

  /**
   * Gets all DiagnosisKeyEntitites that list the country with the origin as {@link DiagnosisKeyEntity} instances.
   *
   * @param country Countrycode for the request
   * @return all DiagnosisKeyEntitites that are from the given country
   */
  public List<DiagnosisKeyEntity> getAllDiagnosisKeyEntityFromOrigin(String country) {
    log.info("Requested all DiagnosisKeyEntitites.");
    return diagnosisKeyEntityRepository.findAllByPayloadOrigin(country);
  }

  /**
   * Gets all DiagnosisKeyEntitites with a specific batchtag.
   *
   * @param batchTag the batchtag for the request
   * @return all DiagnosisKeyEntitites that have the given batchTag
   */
  public List<AuditEntry> getAllDiagnosisKeyEntityByBatchTag(String batchTag) {
    log.info("Requested all DiagnosisKeyEntities by a batchTag.");
    return diagnosisKeyEntityRepository.getAuditInformationByBatchTag(batchTag);
  }

  /**
   * Deletes all {@link DiagnosisKeyEntity} instances that are older than the time parameter.
   *
   * @param time the wich to remove the entities up to
   * @return the number of deleted rows.
   */
  public int deleteAllBefore(ZonedDateTime time) {
    log.info("Start delete all Before {}.", time);
    return diagnosisKeyEntityRepository.deleteByCreatedAtBefore(time);
  }

  public List<DiagnosisKeyEntity> getDiagnosisKeysBatchForCountry(String batchTag, String country) {
    return diagnosisKeyEntityRepository.findByBatchTagIsAndUploader_CountryIsNotOrderByIdAsc(batchTag, country);
  }

  public static class DiagnosisKeyInsertException extends Exception {

    private static final long serialVersionUID = 1L;
    @Getter
    private final HashMap<Integer, List<Integer>> resultMap;

    DiagnosisKeyInsertException(String message, HashMap<Integer, List<Integer>> resultMap) {
      super(message);
      this.resultMap = resultMap;
    }
  }

  public static class DiagnosisKeyDuplicateInsertException extends DiagnosisKeyInsertException {
    DiagnosisKeyDuplicateInsertException(String message, HashMap<Integer, List<Integer>> resultMap) {
      super(message, resultMap);
    }
  }
}
