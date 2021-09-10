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

import static org.mockito.Mockito.verify;

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.entity.CallbackSubscriptionEntity;
import eu.interop.federationgateway.entity.CallbackTaskEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import eu.interop.federationgateway.repository.CallbackSubscriptionRepository;
import eu.interop.federationgateway.repository.CallbackTaskRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyBatchRepository;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CallbackServiceTest {

  @Autowired
  CallbackSubscriptionRepository callbackSubscriptionRepository;

  @Autowired
  CallbackTaskRepository callbackTaskRepository;

  @Autowired
  DiagnosisKeyBatchRepository diagnosisKeyBatchRepository;

  CallbackService callbackService;

  @BeforeEach
  public void setUp() {
     callbackService = new CallbackService(callbackSubscriptionRepository, callbackTaskRepository);
  }

  @BeforeEach
  @AfterEach
  public void teardown() {
    callbackTaskRepository.deleteAll();
    diagnosisKeyBatchRepository.deleteAll();
    callbackSubscriptionRepository.deleteAll();
  }

  @Test
  public void testSaveMethodUpdatesExistingEntries() {
    CallbackSubscriptionEntity entity = new CallbackSubscriptionEntity(
      null, null, TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EXAMPLE, TestData.COUNTRY_A);

    entity = callbackService.saveCallbackSubscription(entity);

    Assertions.assertEquals(1, callbackSubscriptionRepository.count());

    CallbackSubscriptionEntity newEntity = new CallbackSubscriptionEntity(
      null, null, TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EFGS, TestData.COUNTRY_A);

    newEntity = callbackService.saveCallbackSubscription(newEntity);

    Assertions.assertEquals(1, callbackSubscriptionRepository.count());
    Assertions.assertEquals(TestData.CALLBACK_URL_EFGS, callbackSubscriptionRepository.findAll().get(0).getUrl());
    Assertions.assertEquals(entity.getId(), newEntity.getId());

    CallbackSubscriptionEntity entityOtherCountry = new CallbackSubscriptionEntity(
      null, null, TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EFGS, TestData.COUNTRY_B);

    callbackService.saveCallbackSubscription(entityOtherCountry);

    Assertions.assertEquals(2, callbackSubscriptionRepository.count());
  }

  @Test
  public void testDeleteCallbackSubscription() {
    CallbackSubscriptionEntity subscriptionEntity = new CallbackSubscriptionEntity(
      null, null, TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EXAMPLE, TestData.COUNTRY_A);

    subscriptionEntity = callbackService.saveCallbackSubscription(subscriptionEntity);

    DiagnosisKeyBatchEntity batch = new DiagnosisKeyBatchEntity(null, ZonedDateTime.now(), "batchTag", null);
    batch = diagnosisKeyBatchRepository.save(batch);

    CallbackTaskEntity taskEntity = new CallbackTaskEntity(
      null, ZonedDateTime.now(), null, null, 0, null, batch, subscriptionEntity);

    callbackTaskRepository.save(taskEntity);

    Assertions.assertEquals(1, callbackSubscriptionRepository.count());
    Assertions.assertEquals(1, callbackTaskRepository.count());

    callbackService.deleteCallbackSubscription(subscriptionEntity);

    Assertions.assertEquals(0, callbackSubscriptionRepository.count());
    Assertions.assertEquals(0, callbackTaskRepository.count());
  }

  @Test
  public void testGetAllCallbackSubscription() {
    CallbackSubscriptionEntity[] given = {
      new CallbackSubscriptionEntity(
        null, ZonedDateTime.now().withNano(0), TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EXAMPLE, TestData.COUNTRY_A),
      new CallbackSubscriptionEntity(
        null, ZonedDateTime.now().withNano(0), TestData.CALLBACK_ID_SECOND, TestData.CALLBACK_URL_EXAMPLE, TestData.COUNTRY_A),
      new CallbackSubscriptionEntity(
        null, ZonedDateTime.now().withNano(0), TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EXAMPLE, TestData.COUNTRY_B)
    };

    callbackSubscriptionRepository.saveAll(List.of(given));
    Assertions.assertEquals(3, callbackSubscriptionRepository.count());

    List<CallbackSubscriptionEntity> entities = callbackService.getAllCallbackSubscriptions();
    Assertions.assertEquals(3, entities.size());
    Assertions.assertArrayEquals(given, entities.toArray());

    entities = callbackService.getAllCallbackSubscriptionsForCountry(TestData.COUNTRY_A);
    Assertions.assertEquals(2, entities.size());
    Assertions.assertArrayEquals(new CallbackSubscriptionEntity[]{given[0], given[1]}, entities.toArray());

    entities = callbackService.getAllCallbackSubscriptionsForCountry(TestData.COUNTRY_B);
    Assertions.assertEquals(1, entities.size());
    Assertions.assertArrayEquals(new CallbackSubscriptionEntity[]{given[2]}, entities.toArray());
  }

  @Test
  public void testCheckUrlMethod() {
    // check if given string is a url
    Assertions.assertFalse(callbackService.checkUrl("teststring1234", TestData.COUNTRY_A));

    // check for correct protocol (https)
    Assertions.assertFalse(callbackService.checkUrl("http://example.org", TestData.COUNTRY_A));

    // check that url has no query parameters
    Assertions.assertFalse(callbackService.checkUrl("https://example.org?abc=123", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://localhost", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://127.0.0.1", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://10.2.5.6", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://100.64.23.5", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://169.254.85.69", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://172.16.5.3", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://192.168.178.5", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://::1", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://fd9e:35ga:352e:1212::1", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://0", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://0177.0.0.1/asdasd/asdasd/zxc", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://[::]:80", TestData.COUNTRY_A));

    // check that url is not a local target
    Assertions.assertFalse(callbackService.checkUrl("https://0144.0100.0.1/foobar", TestData.COUNTRY_A));

    try {
      InetAddress.getByName("example.org");
      // check that url's hostname is resolved
      Assertions.assertTrue(callbackService.checkUrl("https://example.org", TestData.COUNTRY_A));
    } catch (UnknownHostException ignored) {
    } // skipping positive test case if no name resolution is possible
  }

  @Test
  public void callbackTaskCleanUpShouldCallRepoMethod() {
    CallbackTaskRepository callbackTaskRepositoryMock = Mockito.mock(CallbackTaskRepository.class);
    callbackService = new CallbackService(callbackSubscriptionRepository, callbackTaskRepositoryMock);

    ZonedDateTime timestamp = ZonedDateTime.now();

    ArgumentCaptor<ZonedDateTime> captor = ArgumentCaptor.forClass(ZonedDateTime.class);

    callbackService.removeTaskLocksOlderThan(timestamp);

    verify(callbackTaskRepositoryMock).removeTaskLocksOlderThan(captor.capture());
    Assertions.assertEquals(timestamp, captor.getValue());
  }

  @Test
  public void notBeforePropertyShouldBeSetToPreviousCallbackTask() {
    CallbackSubscriptionEntity callbackSubscription1 = createCallbackSubscriptionEntity("r1");
    CallbackSubscriptionEntity callbackSubscription2 = createCallbackSubscriptionEntity("r2");

    DiagnosisKeyBatchEntity batch = new DiagnosisKeyBatchEntity(null, ZonedDateTime.now(), "batchTag", "batchTag2");
    batch = diagnosisKeyBatchRepository.save(batch);

    DiagnosisKeyBatchEntity batch2 = new DiagnosisKeyBatchEntity(null, ZonedDateTime.now(), "batchTag2", null);
    batch2 = diagnosisKeyBatchRepository.save(batch2);

    callbackService.notifyAllCountriesForNewBatchTag(batch);
    callbackService.notifyAllCountriesForNewBatchTag(batch2);

    List<CallbackTaskEntity> callbackTasks = callbackTaskRepository.findAll();

    Assertions.assertEquals(4, callbackTasks.size());
    Assertions.assertEquals(callbackSubscription1.getId(), callbackTasks.get(0).getCallbackSubscription().getId());
    Assertions.assertEquals(callbackSubscription2.getId(), callbackTasks.get(1).getCallbackSubscription().getId());
    Assertions.assertEquals(callbackSubscription1.getId(), callbackTasks.get(2).getCallbackSubscription().getId());
    Assertions.assertEquals(callbackSubscription2.getId(), callbackTasks.get(3).getCallbackSubscription().getId());

    Assertions.assertNull(callbackTasks.get(0).getNotBefore());
    Assertions.assertNull(callbackTasks.get(1).getNotBefore());
    Assertions.assertEquals(callbackTasks.get(0), callbackTasks.get(2).getNotBefore());
    Assertions.assertEquals(callbackTasks.get(1), callbackTasks.get(3).getNotBefore());
  }

  @Test
  public void notBeforePropertyShouldBeSetToPreviousCallbackTask2() {
    callbackTaskRepository.deleteAll();
    CallbackSubscriptionEntity callbackSubscription1 = createCallbackSubscriptionEntity("r1");

    DiagnosisKeyBatchEntity batch = new DiagnosisKeyBatchEntity(null, ZonedDateTime.now(), "batchTag", "batchTag2");
    batch = diagnosisKeyBatchRepository.save(batch);

    DiagnosisKeyBatchEntity batch2 = new DiagnosisKeyBatchEntity(null, ZonedDateTime.now(), "batchTag2", "batchTag3");
    batch2 = diagnosisKeyBatchRepository.save(batch2);

    DiagnosisKeyBatchEntity batch3 = new DiagnosisKeyBatchEntity(null, ZonedDateTime.now(), "batchTag3", null);
    batch3 = diagnosisKeyBatchRepository.save(batch3);

    callbackService.notifyAllCountriesForNewBatchTag(batch);
    callbackService.notifyAllCountriesForNewBatchTag(batch2);
    callbackService.notifyAllCountriesForNewBatchTag(batch3);

    List<CallbackTaskEntity> callbackTasks = callbackTaskRepository.findAll();

    Assertions.assertEquals(3, callbackTasks.size());
    Assertions.assertEquals(callbackSubscription1.getId(), callbackTasks.get(0).getCallbackSubscription().getId());
    Assertions.assertEquals(callbackSubscription1.getId(), callbackTasks.get(1).getCallbackSubscription().getId());
    Assertions.assertEquals(callbackSubscription1.getId(), callbackTasks.get(2).getCallbackSubscription().getId());

    Assertions.assertNull(callbackTasks.get(0).getNotBefore());
    Assertions.assertEquals(callbackTasks.get(0), callbackTasks.get(1).getNotBefore());
    Assertions.assertEquals(callbackTasks.get(1), callbackTasks.get(2).getNotBefore());
  }

  @Test
  public void notifiyAllCallbackSubscribersMethodShouldCreateACallbackTaskForEachSubscriber() {
    CallbackSubscriptionEntity callbackSubscription1 = createCallbackSubscriptionEntity("r1");
    CallbackSubscriptionEntity callbackSubscription2 = createCallbackSubscriptionEntity("r2");
    CallbackSubscriptionEntity callbackSubscription3 = createCallbackSubscriptionEntity("r3");

    DiagnosisKeyBatchEntity batch = new DiagnosisKeyBatchEntity(null, ZonedDateTime.now(), "batchTag", null);
    batch = diagnosisKeyBatchRepository.save(batch);

    callbackService.notifyAllCountriesForNewBatchTag(batch);

    List<CallbackTaskEntity> callbackTasks = callbackTaskRepository.findAll();

    Assertions.assertEquals(3, callbackTasks.size());
    Assertions.assertEquals(callbackSubscription1.getId(), callbackTasks.get(0).getCallbackSubscription().getId());
    Assertions.assertNull(callbackTasks.get(0).getNotBefore());
    Assertions.assertEquals(callbackSubscription2.getId(), callbackTasks.get(1).getCallbackSubscription().getId());
    Assertions.assertNull(callbackTasks.get(1).getNotBefore());
    Assertions.assertEquals(callbackSubscription3.getId(), callbackTasks.get(2).getCallbackSubscription().getId());
    Assertions.assertNull(callbackTasks.get(2).getNotBefore());
  }

  private CallbackSubscriptionEntity createCallbackSubscriptionEntity(String random) {
    CallbackSubscriptionEntity callbackSubscriptionEntity = new CallbackSubscriptionEntity(null, ZonedDateTime.now(), random, "url", "DE");
    return callbackSubscriptionRepository.save(callbackSubscriptionEntity);
  }


}
