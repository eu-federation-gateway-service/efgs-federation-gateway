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

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyPayload;
import eu.interop.federationgateway.entity.FormatInformation;
import eu.interop.federationgateway.entity.UploaderInformation;
import eu.interop.federationgateway.repository.DiagnosisKeyBatchRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Random;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DiagnosisKeyCleanupServiceTest {

  @Autowired
  DiagnosisKeyBatchRepository diagnosisKeyBatchRepository;

  @Autowired
  DiagnosisKeyEntityRepository diagnosisKeyEntityRepository;

  @Autowired
  DiagnosisKeyCleanupService diagnosisKeyCleanupService;

  @Autowired
  EfgsProperties efgsProperties;

  @Before
  @After
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

    Assert.assertEquals(12, diagnosisKeyEntityRepository.count());

    diagnosisKeyCleanupService.cleanupDiagnosisKeys();

    Assert.assertEquals(7, diagnosisKeyEntityRepository.count());
  }

  @Test
  public void cleanUpServiceShouldDeleteAllDiagnosisKeysBatches() {
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

    Assert.assertEquals(12, diagnosisKeyBatchRepository.count());

    diagnosisKeyCleanupService.cleanupDiagnosisKeys();

    Assert.assertEquals(7, diagnosisKeyBatchRepository.count());
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
