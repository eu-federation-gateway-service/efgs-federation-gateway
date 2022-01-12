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

package eu.interop.federationgateway.repository;

import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DiagnosisKeyBatchRepositoryTest {

  @Autowired
  DiagnosisKeyBatchRepository repository;

  @BeforeEach
  public void setup() {
    repository.deleteAll();
  }

  @Test
  public void testFindFirstByTimestamp() {
    createEntity(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1), "BT1");

    createEntity(ZonedDateTime.now(ZoneOffset.UTC).withHour(14), "BT2");
    createEntity(ZonedDateTime.now(ZoneOffset.UTC).withHour(10), "BT3");
    createEntity(ZonedDateTime.now(ZoneOffset.UTC).withHour(12), "BT4");

    Optional<DiagnosisKeyBatchEntity> queryResult = repository.findFirstByCreatedAtIsGreaterThanOrderByCreatedAtAsc(
      ZonedDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC)
    );

    Assertions.assertTrue(queryResult.isPresent());
    Assertions.assertEquals("BT3", queryResult.get().getBatchName());

    queryResult = repository.findFirstByCreatedAtIsGreaterThanOrderByCreatedAtAsc(
      ZonedDateTime.now(ZoneOffset.UTC).withHour(16)
    );

    Assertions.assertFalse(queryResult.isPresent());
  }

  private void createEntity(ZonedDateTime timestamp, String batchTag) {
    repository.save(new DiagnosisKeyBatchEntity(
      null,
      timestamp,
      batchTag,
      null
    ));
  }


}
