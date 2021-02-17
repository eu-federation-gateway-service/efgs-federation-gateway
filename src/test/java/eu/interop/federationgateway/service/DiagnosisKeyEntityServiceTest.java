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
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.defaultanswers.ReturnsEmptyValues;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;

public class DiagnosisKeyEntityServiceTest {

  private DiagnosisKeyEntityService diagnosisKeyEntityService;

  private DiagnosisKeyEntityRepository diagnosisKeyEntityRepositoryMock;

  @Before
  public void setup() {
    diagnosisKeyEntityRepositoryMock = Mockito.mock(DiagnosisKeyEntityRepository.class);
    this.diagnosisKeyEntityService = new DiagnosisKeyEntityService(diagnosisKeyEntityRepositoryMock);
  }

  @Test
  public void assertThatBatchTagExistsMethodReturnsCorrectAnswer() {
    String dummyBatchTag = "dummyBatchTag";

    when(diagnosisKeyEntityRepositoryMock.countAllByUploader_BatchTag(matches(dummyBatchTag))).thenReturn(5);
    Assert.assertTrue(diagnosisKeyEntityService.uploadBatchTagExists(dummyBatchTag));

    when(diagnosisKeyEntityRepositoryMock.countAllByUploader_BatchTag(matches(dummyBatchTag))).thenReturn(0);
    Assert.assertFalse(diagnosisKeyEntityService.uploadBatchTagExists(dummyBatchTag));
  }

  @Test
  public void assertThatNewEntitiesAreSavedToDatabaseAndCreatedAtTimestampIsSet() throws DiagnosisKeyEntityService.DiagnosisKeyInsertException {
    DiagnosisKeyEntity testEntity = TestData.getDiagnosisKeyTestEntityforCreation();
    DiagnosisKeyEntity testEntity2 = TestData.getDiagnosisKeyTestEntityforCreation();
    DiagnosisKeyEntity testEntity3 = TestData.getDiagnosisKeyTestEntityforCreation();

    diagnosisKeyEntityService.saveDiagnosisKeyEntities(List.of(testEntity, testEntity2, testEntity3));

    ArgumentCaptor<DiagnosisKeyEntity> captor = ArgumentCaptor.forClass(DiagnosisKeyEntity.class);
    verify(diagnosisKeyEntityRepositoryMock, times(3)).save(captor.capture());

    captor.getAllValues().forEach(Assert::assertNotNull);
  }

  @Test
  public void assertThatCreatedAtFieldIsNotChangedWhenAlreadyExists() {
    DiagnosisKeyEntity testEntity = TestData.getDiagnosisKeyTestEntityforCreation();
    ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
    testEntity.setCreatedAt(timestamp);

    diagnosisKeyEntityService.saveDiagnosisKeyEntity(testEntity);

    ArgumentCaptor<DiagnosisKeyEntity> captor = ArgumentCaptor.forClass(DiagnosisKeyEntity.class);
    verify(diagnosisKeyEntityRepositoryMock).save(captor.capture());

    Assert.assertEquals(timestamp, captor.getValue().getCreatedAt());
  }

  @Test(expected = DiagnosisKeyEntityService.DiagnosisKeyInsertException.class)
  public void assertThatFailedInsertationOfMultipleEntitiesThrowsCorrectExceptionDbError() throws DiagnosisKeyEntityService.DiagnosisKeyInsertException {
    when(diagnosisKeyEntityRepositoryMock.save(any()))
      .thenThrow(new QueryTimeoutException("DB is broken"));

    DiagnosisKeyEntity testEntity = TestData.getDiagnosisKeyTestEntityforCreation();
    DiagnosisKeyEntity testEntity2 = TestData.getDiagnosisKeyTestEntityforCreation();
    DiagnosisKeyEntity testEntity3 = TestData.getDiagnosisKeyTestEntityforCreation();

    try {
      diagnosisKeyEntityService.saveDiagnosisKeyEntities(List.of(testEntity, testEntity2, testEntity3));
    } catch (DiagnosisKeyEntityService.DiagnosisKeyInsertException e) {
      Assert.assertTrue(e.getResultMap().get(201).isEmpty());
      Assert.assertTrue(e.getResultMap().get(409).isEmpty());
      Assert.assertTrue(e.getResultMap().get(500).contains(0));
      Assert.assertTrue(e.getResultMap().get(500).contains(1));
      Assert.assertTrue(e.getResultMap().get(500).contains(2));

      verify(diagnosisKeyEntityRepositoryMock, times(3)).save(any());
      throw e;
    }
  }

  @Test(expected = DiagnosisKeyEntityService.DiagnosisKeyInsertException.class)
  public void assertThatFailedInsertationOfMultipleEntitiesThrowsCorrectExceptionOnIntegrityCheck() throws DiagnosisKeyEntityService.DiagnosisKeyInsertException {
    when(diagnosisKeyEntityRepositoryMock.save(any()))
      .thenReturn(null)
      .thenThrow(new DataIntegrityViolationException("test"))
      .thenReturn(null);

    DiagnosisKeyEntity testEntity = TestData.getDiagnosisKeyTestEntityforCreation();
    DiagnosisKeyEntity testEntity2 = TestData.getDiagnosisKeyTestEntityforCreation();
    DiagnosisKeyEntity testEntity3 = TestData.getDiagnosisKeyTestEntityforCreation();

    try {
      diagnosisKeyEntityService.saveDiagnosisKeyEntities(List.of(testEntity, testEntity2, testEntity3));
    } catch (DiagnosisKeyEntityService.DiagnosisKeyInsertException e) {
      Assert.assertTrue(e.getResultMap().get(201).contains(0));
      Assert.assertTrue(e.getResultMap().get(201).contains(2));
      Assert.assertTrue(e.getResultMap().get(500).isEmpty());
      Assert.assertTrue(e.getResultMap().get(409).contains(1));

      verify(diagnosisKeyEntityRepositoryMock, times(3)).save(any());
      throw e;
    }
  }


  @Test
  public void assertThatAllEntitiesHaveTheSameUploadTimestamp() throws DiagnosisKeyEntityService.DiagnosisKeyInsertException {


    when(diagnosisKeyEntityRepositoryMock.save(any()))
      .thenAnswer(new AnswersWithDelay(100, new ReturnsEmptyValues()));

    DiagnosisKeyEntity testEntity = TestData.getDiagnosisKeyTestEntityforCreation();
    DiagnosisKeyEntity testEntity2 = TestData.getDiagnosisKeyTestEntityforCreation();
    DiagnosisKeyEntity testEntity3 = TestData.getDiagnosisKeyTestEntityforCreation();

    diagnosisKeyEntityService.saveDiagnosisKeyEntities(List.of(testEntity, testEntity2, testEntity3));

    ArgumentCaptor<DiagnosisKeyEntity> captor = ArgumentCaptor.forClass(DiagnosisKeyEntity.class);

    verify(diagnosisKeyEntityRepositoryMock, times(3)).save(captor.capture());

    ZonedDateTime firstTimestamp = captor.getAllValues().get(0).getCreatedAt();

    captor.getAllValues().forEach(entity -> {
      Assert.assertEquals(firstTimestamp, entity.getCreatedAt());
    });
  }

  @Test
  public void testGetAllMethod() {
    diagnosisKeyEntityService.getAllDiagnosisKeyEntity();
    verify(diagnosisKeyEntityRepositoryMock).findAll();
  }

  @Test
  public void testGetAllFromOriginMethod() {
    diagnosisKeyEntityService.getAllDiagnosisKeyEntityFromOrigin("test");
    verify(diagnosisKeyEntityRepositoryMock).findAllByPayloadOrigin(matches("test"));
  }

  @Test
  public void testDeleteAllBeforeMethod() {
    ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
    diagnosisKeyEntityService.deleteAllBefore(timestamp);
    verify(diagnosisKeyEntityRepositoryMock).deleteByCreatedAtBefore(eq(timestamp));
  }

  @Test
  public void testBatchForCountry() {
    diagnosisKeyEntityService.getDiagnosisKeysBatchForCountry(TestData.FIRST_BATCHTAG, TestData.COUNTRY_A);
    verify(diagnosisKeyEntityRepositoryMock).findByBatchTagIsAndUploader_CountryIsNotOrderByIdAsc(
      eq(TestData.FIRST_BATCHTAG),
      eq(TestData.COUNTRY_A)
    );
  }

}
