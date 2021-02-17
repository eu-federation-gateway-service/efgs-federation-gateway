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

package eu.interop.federationgateway.repository;

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DiagnosisKeyEntityRepositoryTest {

  @Autowired
  DiagnosisKeyEntityRepository repository;

  @Before
  public void setup() {
    repository.deleteAll();

    DiagnosisKeyEntity e1 = TestData.getDiagnosisKeyTestEntityforCreation();
    DiagnosisKeyEntity e2 = TestData.getDiagnosisKeyTestEntityforCreation();
    DiagnosisKeyEntity e3 = TestData.getDiagnosisKeyTestEntityforCreation();

    e1.setPayloadHash("a");
    e2.setPayloadHash("b");
    e3.setPayloadHash("c");

    e1.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
    e2.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
    e3.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));

    e1.getUploader().setBatchTag(TestData.FIRST_BATCHTAG);
    e2.getUploader().setBatchTag(TestData.FIRST_BATCHTAG);
    e3.getUploader().setBatchTag(TestData.SECOND_BATCHTAG);

    e1.setBatchTag(TestData.FIRST_BATCHTAG);
    e2.setBatchTag(TestData.FIRST_BATCHTAG);
    e3.setBatchTag(TestData.SECOND_BATCHTAG);

    e1.getUploader().setCountry(TestData.COUNTRY_A);
    e2.getUploader().setCountry(TestData.COUNTRY_B);
    e3.getUploader().setCountry(TestData.COUNTRY_B);

    e1.getPayload().setOrigin(TestData.FIRST_ORIGIN);
    e2.getPayload().setOrigin(TestData.SECOND_ORIGIN);
    e3.getPayload().setOrigin(TestData.SECOND_ORIGIN);

    e1.getPayload().setVisitedCountries(TestData.VISITED_COUNTRIES_PLUS_ONE);
    e2.getPayload().setVisitedCountries(TestData.VISITED_COUNTRIES);
    e3.getPayload().setVisitedCountries(TestData.VISITED_COUNTRIES);

    e3.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC).minusDays(5));

    repository.save(e1);
    repository.save(e2);
    repository.save(e3);
  }

  @Test
  public void testFindByPayloadOrigin() {
    List<DiagnosisKeyEntity> result = repository.findAllByPayloadOrigin(TestData.FIRST_ORIGIN);

    Assert.assertEquals(1, result.size());
    Assert.assertEquals(TestData.FIRST_ORIGIN, result.get(0).getPayload().getOrigin());

    result = repository.findAllByPayloadOrigin(TestData.SECOND_ORIGIN);

    Assert.assertEquals(2, result.size());
    Assert.assertEquals(TestData.SECOND_ORIGIN, result.get(0).getPayload().getOrigin());
    Assert.assertEquals(TestData.SECOND_ORIGIN, result.get(1).getPayload().getOrigin());
  }

  @Test
  public void testFindByBatchTag() {
    Assert.assertEquals(2, repository.countAllByUploader_BatchTag(TestData.FIRST_BATCHTAG));
    Assert.assertEquals(1, repository.countAllByUploader_BatchTag(TestData.SECOND_BATCHTAG));
  }

  @Test
  public void testFindFirstByBatchTagIsNull() {
    DiagnosisKeyEntity e1 = TestData.getDiagnosisKeyTestEntityforCreation();
    DiagnosisKeyEntity e2 = TestData.getDiagnosisKeyTestEntityforCreation();

    e1.setPayloadHash("x");
    e2.setPayloadHash("y");

    e1.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
    e2.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));

    repository.save(e1);
    repository.save(e2);

    Optional<DiagnosisKeyEntity> result = repository.findFirstByBatchTagIsNull();

    Assert.assertTrue(result.isPresent());
    Assert.assertEquals("x", result.get().getPayloadHash());
  }

  @Test
  public void testFindByBatchTagIsAndUploader_CountryIsNot() {
    List<DiagnosisKeyEntity> result = repository.findByBatchTagIsAndUploader_CountryIsNotOrderByIdAsc(TestData.FIRST_BATCHTAG, TestData.COUNTRY_A);
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(TestData.COUNTRY_B, result.get(0).getUploader().getCountry());
    Assert.assertEquals(TestData.FIRST_BATCHTAG, result.get(0).getBatchTag());

    result = repository.findByBatchTagIsAndUploader_CountryIsNotOrderByIdAsc(TestData.FIRST_BATCHTAG, TestData.COUNTRY_B);
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(TestData.COUNTRY_A, result.get(0).getUploader().getCountry());
    Assert.assertEquals(TestData.FIRST_BATCHTAG, result.get(0).getBatchTag());

    result = repository.findByBatchTagIsAndUploader_CountryIsNotOrderByIdAsc(TestData.SECOND_BATCHTAG, TestData.COUNTRY_A);
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(TestData.COUNTRY_B, result.get(0).getUploader().getCountry());
    Assert.assertEquals(TestData.SECOND_BATCHTAG, result.get(0).getBatchTag());

    result = repository.findByBatchTagIsAndUploader_CountryIsNotOrderByIdAsc(TestData.SECOND_BATCHTAG, TestData.COUNTRY_B);
    Assert.assertTrue(result.isEmpty());
  }


}
