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

import eu.interop.federationgateway.entity.CallbackSubscriptionEntity;
import eu.interop.federationgateway.entity.CallbackTaskEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
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
public class CallbackTaskRepositoryTest {

  @Autowired
  CallbackTaskRepository repository;

  @Autowired
  CallbackSubscriptionRepository subscriptionRepository;

  @Autowired
  DiagnosisKeyBatchRepository diagnosisKeyBatchRepository;

  @Autowired
  CertificateRepository certificateRepository;

  @Before
  @After
  public void setup() {

    repository.deleteAll();
    subscriptionRepository.deleteAll();
    diagnosisKeyBatchRepository.deleteAll();
    certificateRepository.deleteAll();

  }

  @Test
  public void testRemoveTaskLocksFromAbandonedTasks() {
    createEntity(ZonedDateTime.now(ZoneOffset.UTC), "a");
    createEntity(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(3), "b");
    createEntity(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(4), "c");
    createEntity(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(6), "d");
    createEntity(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(8), "e");

    Assert.assertEquals(5, repository.count());
    Assert.assertEquals(5, countNonNullTaskLocks(repository.findAll()));

    ZonedDateTime timestamp = ZonedDateTime.now().minusMinutes(5);
    int updateCount = repository.removeTaskLocksOlderThan(timestamp);

    Assert.assertEquals(2, updateCount);
    Assert.assertEquals(5, repository.count());
    Assert.assertEquals(3, countNonNullTaskLocks(repository.findAll()));

  }

  private long countNonNullTaskLocks(List<CallbackTaskEntity> entities) {
    return entities.stream()
      .filter(e -> e.getExecutionLock() != null)
      .count();
  }

  private void createEntity(ZonedDateTime timestamp, String random) {
    DiagnosisKeyBatchEntity diagnosisKeyBatchEntity = new DiagnosisKeyBatchEntity(null, ZonedDateTime.now(), random + "batch", null);
    diagnosisKeyBatchEntity = diagnosisKeyBatchRepository.save(diagnosisKeyBatchEntity);

    CallbackSubscriptionEntity callbackSubscriptionEntity = new CallbackSubscriptionEntity(null, ZonedDateTime.now(), random, random + "url", "DE");
    callbackSubscriptionEntity = subscriptionRepository.save(callbackSubscriptionEntity);

    repository.save(new CallbackTaskEntity(null, ZonedDateTime.now(), timestamp, null, 0, null, diagnosisKeyBatchEntity, callbackSubscriptionEntity));
  }


}
