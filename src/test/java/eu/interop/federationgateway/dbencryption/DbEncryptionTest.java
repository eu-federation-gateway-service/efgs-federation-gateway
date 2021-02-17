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

package eu.interop.federationgateway.dbencryption;

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import java.time.ZonedDateTime;
import javax.persistence.EntityManager;
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
public class DbEncryptionTest {

  @Autowired
  DiagnosisKeyEntityRepository diagnosisKeyEntityRepository;

  @Autowired
  EntityManager entityManager;

  @Before
  @After
  public void setup() {
    diagnosisKeyEntityRepository.deleteAll();
  }

  @Test
  public void testThatDiagnosisKeyDataIsStoredEncrypted() {
    DiagnosisKeyEntity entity = TestData.getDiagnosisKeyTestEntityforCreation();
    entity.setCreatedAt(ZonedDateTime.now());
    entity = diagnosisKeyEntityRepository.save(entity);

    Object databaseEntry = entityManager.createNativeQuery("SELECT "
      + "d.payload_key_data, d.payload_rolling_start_interval_number, "
      + "d.payload_rolling_period, d.payload_transmission_risk_level, "
      + "d.payload_visited_countries, d.payload_origin, d.payload_report_type, "
      + "d.payload_days_since_onset_of_symptoms FROM diagnosiskey d WHERE ID=:id")
      .setParameter("id", entity.getId())
      .getSingleResult();

    Assert.assertTrue(((Object[]) databaseEntry)[0] instanceof String);
    Assert.assertNotEquals(entity.getPayload().getRollingStartIntervalNumber(), ((Object[]) databaseEntry)[1]);
    Assert.assertNotEquals(entity.getPayload().getRollingPeriod(), ((Object[]) databaseEntry)[2]);
    Assert.assertNotEquals(entity.getPayload().getTransmissionRiskLevel(), ((Object[]) databaseEntry)[3]);
    Assert.assertNotEquals(entity.getPayload().getVisitedCountries(), ((Object[]) databaseEntry)[4]);
    Assert.assertNotEquals(entity.getPayload().getOrigin(), ((Object[]) databaseEntry)[5]);
    Assert.assertNotEquals(entity.getPayload().getReportType().ordinal(), ((Object[]) databaseEntry)[6]);
    Assert.assertNotEquals(entity.getPayload().getDaysSinceOnsetOfSymptoms(), ((Object[]) databaseEntry)[7]);
  }

  @Test
  public void testThatDiagnosisKeyDataIsDecryptedAfterStoring() {
    DiagnosisKeyEntity entity = TestData.getDiagnosisKeyTestEntityforCreation();
    entity.setCreatedAt(ZonedDateTime.now());

    DiagnosisKeyEntity savedEntity = diagnosisKeyEntityRepository.save(entity);
    DiagnosisKeyEntity gotEntity = diagnosisKeyEntityRepository.findById(savedEntity.getId()).get();

    Assert.assertEquals(entity.getPayload(), gotEntity.getPayload());
  }
}
