/*-
 * ---license-start
 * EU-Federation-Gateway-Service / efgs-federation-gateway
 * ---
 * Copyright (C) 2020 - 2022 T-Systems International GmbH and all other contributors
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

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyCleanupEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyPayload;
import eu.interop.federationgateway.entity.FormatInformation;
import eu.interop.federationgateway.entity.UploaderInformation;
import eu.interop.federationgateway.repository.DiagnosisKeyBatchRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyCleanupRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DiagnosisKeyCleanupServiceTest {

  @Autowired
  DiagnosisKeyBatchRepository diagnosisKeyBatchRepository;

  @Autowired
  DiagnosisKeyEntityRepository diagnosisKeyEntityRepository;

  @Autowired
  DiagnosisKeyCleanupService diagnosisKeyCleanupService;

  @Autowired
  DiagnosisKeyCleanupRepository diagnosisKeyCleanupRepository;

  @Autowired
  EfgsProperties efgsProperties;

  @BeforeEach
  @AfterEach
  public void cleanup() {
    diagnosisKeyBatchRepository.deleteAll();
    diagnosisKeyEntityRepository.deleteAll();
  }

  @Test
  public void cleanUpServiceShouldDeleteAllDiagnosisKeys() {
    final int retentionDays = efgsProperties.getDownloadSettings().getMaxAgeInDays();

    ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).withHour(14);

    createDiagnosisKey(timestamp.minusDays(1));
    createDiagnosisKey(timestamp.minusDays(1));
    createDiagnosisKey(timestamp.minusDays(1));
    createDiagnosisKey(timestamp.minusDays(1));

    createDiagnosisKey(timestamp.minusDays(retentionDays));
    createDiagnosisKey(timestamp.minusDays(retentionDays));
    createDiagnosisKey(timestamp.minusDays(retentionDays));

    createDiagnosisKey(timestamp.minusDays(retentionDays + 1));
    createDiagnosisKey(timestamp.minusDays(retentionDays + 1));
    createDiagnosisKey(timestamp.minusDays(retentionDays + 1));
    createDiagnosisKey(timestamp.minusDays(retentionDays + 1));
    createDiagnosisKey(timestamp.minusDays(retentionDays + 1));

    Assertions.assertEquals(12, diagnosisKeyEntityRepository.count());
    diagnosisKeyCleanupService.cleanupDiagnosisKeys();

    List<DiagnosisKeyCleanupEntity> diagnosisKeyCleanupEntities = diagnosisKeyCleanupRepository.findAll();
    Assertions.assertEquals(1, diagnosisKeyCleanupEntities.size());
    Assertions.assertEquals(5,diagnosisKeyCleanupEntities.get(0).getNumberOfKeys());
    Assertions.assertEquals(12,diagnosisKeyCleanupEntities.get(0).getKeysBefore());
    Assertions.assertEquals(7,diagnosisKeyCleanupEntities.get(0).getKeysAfter());

    Assertions.assertEquals(7, diagnosisKeyEntityRepository.count());
  }

  @Test
  public void cleanUpServiceShouldNotDeleteAllDiagnosisKeysBatches() {
    final int retentionDays = efgsProperties.getDownloadSettings().getMaxAgeInDays();

    ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).withHour(14);

    createDiagnosisKeyBatch(timestamp.minusDays(1));
    createDiagnosisKeyBatch(timestamp.minusDays(1));
    createDiagnosisKeyBatch(timestamp.minusDays(1));
    createDiagnosisKeyBatch(timestamp.minusDays(1));

    createDiagnosisKeyBatch(timestamp.minusDays(retentionDays));
    createDiagnosisKeyBatch(timestamp.minusDays(retentionDays));
    createDiagnosisKeyBatch(timestamp.minusDays(retentionDays));

    createDiagnosisKeyBatch(timestamp.minusDays(retentionDays + 1));
    createDiagnosisKeyBatch(timestamp.minusDays(retentionDays + 1));
    createDiagnosisKeyBatch(timestamp.minusDays(retentionDays + 1));
    createDiagnosisKeyBatch(timestamp.minusDays(retentionDays + 1));
    createDiagnosisKeyBatch(timestamp.minusDays(retentionDays + 1));

    Assertions.assertEquals(12, diagnosisKeyBatchRepository.count());

    diagnosisKeyCleanupService.cleanupDiagnosisKeys();

    Assertions.assertEquals(12, diagnosisKeyBatchRepository.count());
  }

  private DiagnosisKeyBatchEntity createDiagnosisKeyBatch(ZonedDateTime createdAt) {
    Random random = new Random();
    return diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(
      null,
      createdAt,
      String.valueOf(random.nextInt()),
      null
    ));
  }

  private DiagnosisKeyEntity createDiagnosisKey(ZonedDateTime createdAt) {
    Random random = new Random();
    return diagnosisKeyEntityRepository.save(new DiagnosisKeyEntity(
      null,
      createdAt,
      null,
      String.valueOf(random.nextInt()),
      new DiagnosisKeyPayload(
        new byte[0],
        0,
        0,
        0,
        "",
        "",
        DiagnosisKeyPayload.ReportType.SELF_REPORT,
        0
      ),
      new FormatInformation(1, 0),
      new UploaderInformation(
        TestData.FIRST_BATCHTAG,
        "",
        "",
        "",
        ""
      )
    ));
  }
}
