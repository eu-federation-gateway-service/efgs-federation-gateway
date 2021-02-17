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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CallbackServiceTest {

  @Autowired
  CallbackSubscriptionRepository callbackSubscriptionRepository;

  @Autowired
  CallbackTaskRepository callbackTaskRepository;

  @Autowired
  DiagnosisKeyBatchRepository diagnosisKeyBatchRepository;

  CallbackService callbackService;

  @Before
  public void setUp() {
     callbackService = new CallbackService(callbackSubscriptionRepository, callbackTaskRepository);
  }

  @Before
  @After
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

    Assert.assertEquals(1, callbackSubscriptionRepository.count());

    CallbackSubscriptionEntity newEntity = new CallbackSubscriptionEntity(
      null, null, TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EFGS, TestData.COUNTRY_A);

    newEntity = callbackService.saveCallbackSubscription(newEntity);

    Assert.assertEquals(1, callbackSubscriptionRepository.count());
    Assert.assertEquals(TestData.CALLBACK_URL_EFGS, callbackSubscriptionRepository.findAll().get(0).getUrl());
    Assert.assertEquals(entity.getId(), newEntity.getId());

    CallbackSubscriptionEntity entityOtherCountry = new CallbackSubscriptionEntity(
      null, null, TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EFGS, TestData.COUNTRY_B);

    callbackService.saveCallbackSubscription(entityOtherCountry);

    Assert.assertEquals(2, callbackSubscriptionRepository.count());
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

    Assert.assertEquals(1, callbackSubscriptionRepository.count());
    Assert.assertEquals(1, callbackTaskRepository.count());

    callbackService.deleteCallbackSubscription(subscriptionEntity);

    Assert.assertEquals(0, callbackSubscriptionRepository.count());
    Assert.assertEquals(0, callbackTaskRepository.count());
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
    Assert.assertEquals(3, callbackSubscriptionRepository.count());

    List<CallbackSubscriptionEntity> entities = callbackService.getAllCallbackSubscriptions();
    Assert.assertEquals(3, entities.size());
    Assert.assertArrayEquals(given, entities.toArray());

    entities = callbackService.getAllCallbackSubscriptionsForCountry(TestData.COUNTRY_A);
    Assert.assertEquals(2, entities.size());
    Assert.assertArrayEquals(new CallbackSubscriptionEntity[]{given[0], given[1]}, entities.toArray());

    entities = callbackService.getAllCallbackSubscriptionsForCountry(TestData.COUNTRY_B);
    Assert.assertEquals(1, entities.size());
    Assert.assertArrayEquals(new CallbackSubscriptionEntity[]{given[2]}, entities.toArray());
  }

  @Test
  public void testCheckUrlMethod() {
    // check if given string is a url
    Assert.assertFalse(callbackService.checkUrl("teststring1234", TestData.COUNTRY_A));

    // check for correct protocol (https)
    Assert.assertFalse(callbackService.checkUrl("http://example.org", TestData.COUNTRY_A));

    // check that url has no query parameters
    Assert.assertFalse(callbackService.checkUrl("https://example.org?abc=123", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackService.checkUrl("https://localhost", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackService.checkUrl("https://127.0.0.1", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackService.checkUrl("https://10.2.5.6", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackService.checkUrl("https://100.64.23.5", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackService.checkUrl("https://169.254.85.69", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackService.checkUrl("https://172.16.5.3", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackService.checkUrl("https://192.168.178.5", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackService.checkUrl("https://::1", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackService.checkUrl("https://fd9e:35ga:352e:1212::1", TestData.COUNTRY_A));

    try {
      InetAddress.getByName("example.org");
      // check that url's hostname is resolved
      Assert.assertTrue(callbackService.checkUrl("https://example.org", TestData.COUNTRY_A));
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
    Assert.assertEquals(timestamp, captor.getValue());
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

    Assert.assertEquals(4, callbackTasks.size());
    Assert.assertEquals(callbackSubscription1.getId(), callbackTasks.get(0).getCallbackSubscription().getId());
    Assert.assertEquals(callbackSubscription2.getId(), callbackTasks.get(1).getCallbackSubscription().getId());
    Assert.assertEquals(callbackSubscription1.getId(), callbackTasks.get(2).getCallbackSubscription().getId());
    Assert.assertEquals(callbackSubscription2.getId(), callbackTasks.get(3).getCallbackSubscription().getId());

    Assert.assertNull(callbackTasks.get(0).getNotBefore());
    Assert.assertNull(callbackTasks.get(1).getNotBefore());
    Assert.assertEquals(callbackTasks.get(0), callbackTasks.get(2).getNotBefore());
    Assert.assertEquals(callbackTasks.get(1), callbackTasks.get(3).getNotBefore());
  }

  @Test
  public void notBeforePropertyShouldBeSetToPreviousCallbackTask2() {
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

    Assert.assertEquals(3, callbackTasks.size());
    Assert.assertEquals(callbackSubscription1.getId(), callbackTasks.get(0).getCallbackSubscription().getId());
    Assert.assertEquals(callbackSubscription1.getId(), callbackTasks.get(1).getCallbackSubscription().getId());
    Assert.assertEquals(callbackSubscription1.getId(), callbackTasks.get(2).getCallbackSubscription().getId());

    Assert.assertNull(callbackTasks.get(0).getNotBefore());
    Assert.assertEquals(callbackTasks.get(0), callbackTasks.get(1).getNotBefore());
    Assert.assertEquals(callbackTasks.get(1), callbackTasks.get(2).getNotBefore());
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

    Assert.assertEquals(3, callbackTasks.size());
    Assert.assertEquals(callbackSubscription1.getId(), callbackTasks.get(0).getCallbackSubscription().getId());
    Assert.assertNull(callbackTasks.get(0).getNotBefore());
    Assert.assertEquals(callbackSubscription2.getId(), callbackTasks.get(1).getCallbackSubscription().getId());
    Assert.assertNull(callbackTasks.get(1).getNotBefore());
    Assert.assertEquals(callbackSubscription3.getId(), callbackTasks.get(2).getCallbackSubscription().getId());
    Assert.assertNull(callbackTasks.get(2).getNotBefore());
  }

  private CallbackSubscriptionEntity createCallbackSubscriptionEntity(String random) {
    CallbackSubscriptionEntity callbackSubscriptionEntity = new CallbackSubscriptionEntity(null, ZonedDateTime.now(), random, "url", "DE");
    return callbackSubscriptionRepository.save(callbackSubscriptionEntity);
  }


}
