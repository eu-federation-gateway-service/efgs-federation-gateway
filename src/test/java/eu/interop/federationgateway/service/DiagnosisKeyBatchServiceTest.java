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
import eu.interop.federationgateway.repository.DiagnosisKeyBatchRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.defaultanswers.ReturnsEmptyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * This is the test class for the batch service.
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(
  properties = {
    "efgs.batching.doclimit=9",
    "efgs.upload-settings.maximum-upload-batch-size=9"
  }
)
public class DiagnosisKeyBatchServiceTest {

  private final ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneOffset.UTC);
  private final String formattedDate = currentDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
  @Autowired
  private DiagnosisKeyBatchRepository batchRepository;
  @Autowired
  private DiagnosisKeyEntityRepository keyRepository;
  @Autowired
  private EfgsProperties efgsProperties;

  private DiagnosisKeyBatchService batchService;

  private TransactionalDiagnosisKeyBatchService transactionalBatchService;

  private CallbackService callbackServiceMock;

  @Before
  public void before() {
    batchRepository.deleteAll();
    keyRepository.deleteAll();

    callbackServiceMock = Mockito.mock(CallbackService.class);
    transactionalBatchService = new TransactionalDiagnosisKeyBatchService(efgsProperties, keyRepository, batchRepository, callbackServiceMock);
    batchService = new DiagnosisKeyBatchService(efgsProperties, batchRepository, transactionalBatchService);
  }

  /**
   * Test the service scheduling and the batchDocuments method, of class DiagnosisKeyBatchService.
   * The batch repo is empty.
   *
   * @throws Exception if the test cannot be performed.
   */
  @Test
  public void testBatchDocumentsByEmptyRepo() throws Exception {
    log.info("process testBatchDocumentsByEmptyRepo()");
    // save test keys
    List<DiagnosisKeyEntity> entries = keyRepository.saveAll(TestData.createTestDiagKeysWithoutBatchTag());

    batchService.batchDocuments();

    Assert.assertEquals("error batch repo expect one entry", 1, batchRepository.count());

    // check repos
    Assert.assertNull(batchRepository.findAll().get(0).getBatchLink());
    Assert.assertEquals(formattedDate + "-1", batchRepository.findAll().get(0).getBatchName());
    Assert.assertEquals("error to find 3 test keys", 3, keyRepository.count());
    Assert.assertEquals(formattedDate + "-1", keyRepository.findAll().get(0).getBatchTag());
    Assert.assertEquals(formattedDate + "-1", keyRepository.findAll().get(1).getBatchTag());
    Assert.assertEquals(formattedDate + "-1", keyRepository.findAll().get(2).getBatchTag());
  }

  /**
   * Test of batchDocuments method, of class DiagnosisKeyBatchService.
   * The batch repo is filled.
   *
   * @throws Exception if the test cannot be performed.
   */
  @Test
  public void testBatchDocumentsByFilledRepos() throws Exception {
    log.info("process testBatchDocumentsByFilledRepos()");
    // save test keys
    keyRepository.saveAll(TestData.createTestDiagKeysWithoutBatchTag());
    keyRepository.saveAll(TestData.createTestDiagKeyWithBatchTag());

    String batchName = formattedDate + "-1";
    batchRepository.save(new DiagnosisKeyBatchEntity(null, ZonedDateTime.now(ZoneOffset.UTC), batchName, null));

    batchService.batchDocuments();

    Assert.assertEquals("error batch repo expect 2 entries", 2, batchRepository.count());

    // check repos
    Assert.assertEquals(formattedDate + "-2", batchRepository.findAll().get(0).getBatchLink());
    Assert.assertEquals(formattedDate + "-2", batchRepository.findAll().get(1).getBatchName());

    Assert.assertEquals("error to find 4 test keys", 4, keyRepository.count());
    Assert.assertEquals(formattedDate + "-2", keyRepository.findAll().get(0).getBatchTag());
    Assert.assertEquals(formattedDate + "-2", keyRepository.findAll().get(1).getBatchTag());
    Assert.assertEquals(formattedDate + "-2", keyRepository.findAll().get(2).getBatchTag());
    Assert.assertEquals(TestData.TEST_BATCH_TAG_2015616, keyRepository.findAll().get(3).getBatchTag());

    ArgumentCaptor<DiagnosisKeyBatchEntity> captor = ArgumentCaptor.forClass(DiagnosisKeyBatchEntity.class);
    Mockito.verify(callbackServiceMock).notifyAllCountriesForNewBatchTag(captor.capture());
    Assert.assertEquals(formattedDate + "-2", captor.getValue().getBatchName());
  }

  /**
   * Test of batchDocuments method, of class DiagnosisKeyBatchService.
   * The doc limitation for batching test.
   *
   * @throws Exception if the test cannot be performed.
   */
  @Test
  public void testBatchDocumentsForLimitation() throws Exception {
    log.info("process testBatchDocumentsForLimitation()");
    // save test keys
    keyRepository.saveAll(TestData.createTestDiagKeysList(5, "uploaderBatchTag_DE", "DE"));
    keyRepository.saveAll(TestData.createTestDiagKeysList(5, "uploaderBatchTag_PL", "PL"));
    keyRepository.saveAll(TestData.createTestDiagKeysList(5, "uploaderBatchTag_ES", "ES"));
    keyRepository.saveAll(TestData.createTestDiagKeysList(5, "uploaderBatchTag_BG", "BG"));
    keyRepository.saveAll(TestData.createTestDiagKeysList(3, "uploaderBatchTag_BG_1", "BG1"));

    batchRepository.deleteAll();

    batchService.batchDocuments();

    // the batch repo count should be 4 (keys/batch 5,5,5,8) regarding doc limit is 9
    Assert.assertEquals(4, batchRepository.count());

    // check repos
    Assert.assertEquals(formattedDate + "-2", batchRepository.findAll().get(0).getBatchLink());
    Assert.assertEquals(formattedDate + "-2", batchRepository.findAll().get(1).getBatchName());

    Assert.assertEquals("error to find 23 test keys", 23, keyRepository.count());
    Assert.assertEquals(formattedDate + "-1", keyRepository.findAll().get(0).getBatchTag());
    Assert.assertEquals(formattedDate + "-2", keyRepository.findAll().get(5).getBatchTag());
    Assert.assertEquals(formattedDate + "-3", keyRepository.findAll().get(10).getBatchTag());
    Assert.assertEquals(formattedDate + "-4", keyRepository.findAll().get(17).getBatchTag());
  }

  @Test
  public void diagnosisKeysFormatVersionShouldNotBeMixed() throws Exception {
    // save test keys
    keyRepository.saveAll(TestData.createTestDiagKeysList(2, "uploaderBatchTag_DE", "DE", 1, 0));
    keyRepository.saveAll(TestData.createTestDiagKeysList(2, "uploaderBatchTag_PL", "PL", 1, 0));
    keyRepository.saveAll(TestData.createTestDiagKeysList(2, "uploaderBatchTag_ES", "ES", 1, 1));
    keyRepository.saveAll(TestData.createTestDiagKeysList(2, "uploaderBatchTag_BG", "BG", 1, 0));

    batchRepository.deleteAll();

    batchService.batchDocuments();

    // Expect 3 batches: 1: DE,PL 2: ES 3: BG
    Assert.assertEquals(3, batchRepository.count());

    Assert.assertEquals(formattedDate + "-1", batchRepository.findAll().get(0).getBatchName());
    Assert.assertEquals(formattedDate + "-2", batchRepository.findAll().get(1).getBatchName());
    Assert.assertEquals(formattedDate + "-3", batchRepository.findAll().get(2).getBatchName());


    Assert.assertEquals(formattedDate + "-1", keyRepository.findAll().get(0).getBatchTag());
    Assert.assertEquals(formattedDate + "-1", keyRepository.findAll().get(1).getBatchTag());
    Assert.assertEquals(formattedDate + "-1", keyRepository.findAll().get(2).getBatchTag());
    Assert.assertEquals(formattedDate + "-1", keyRepository.findAll().get(3).getBatchTag());

    Assert.assertEquals(formattedDate + "-2", keyRepository.findAll().get(4).getBatchTag());
    Assert.assertEquals(formattedDate + "-2", keyRepository.findAll().get(5).getBatchTag());

    Assert.assertEquals(formattedDate + "-3", keyRepository.findAll().get(6).getBatchTag());
    Assert.assertEquals(formattedDate + "-3", keyRepository.findAll().get(7).getBatchTag());
  }


  @Test
  public void documentBatchingShouldBeStoppedIfTimelimitIsReached() throws Exception {
    // save test keys
    keyRepository.saveAll(TestData.createTestDiagKeysList(5, "uploaderBatchTag_DE", "DE"));
    keyRepository.saveAll(TestData.createTestDiagKeysList(5, "uploaderBatchTag_PL", "PL"));

    batchRepository.deleteAll();

    Mockito
      .doAnswer(new AnswersWithDelay(100, new ReturnsEmptyValues()))
      .when(callbackServiceMock).notifyAllCountriesForNewBatchTag(Mockito.any(DiagnosisKeyBatchEntity.class));

    int defaultTimeLimit = efgsProperties.getBatching().getTimelimit();
    efgsProperties.getBatching().setTimelimit(50);

    batchService.batchDocuments();

    efgsProperties.getBatching().setTimelimit(defaultTimeLimit);

    // the batch repo count should be 1 because batching has stopped after first batch
    Assert.assertEquals(1, batchRepository.count());

    // check repos
    Assert.assertNull(batchRepository.findAll().get(0).getBatchLink());
    Assert.assertEquals(formattedDate + "-1", batchRepository.findAll().get(0).getBatchName());

    Assert.assertEquals(formattedDate + "-1", keyRepository.findAll().get(0).getBatchTag());
    Assert.assertNull(keyRepository.findAll().get(5).getBatchTag());
  }
}
