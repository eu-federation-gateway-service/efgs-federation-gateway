/*-
 * ---license-start
 * EU-Federation-Gateway-Service / efgs-federation-gateway
 * ---
 * Copyright (C) 2020 T-Systems International GmbH and all other contributors
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

import eu.interop.federationgateway.entity.CallbackSubscriptionEntity;
import eu.interop.federationgateway.entity.CallbackTaskEntity;
import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import eu.interop.federationgateway.repository.CallbackSubscriptionRepository;
import eu.interop.federationgateway.repository.CallbackTaskRepository;
import eu.interop.federationgateway.repository.CertificateRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyBatchRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class CallbackTaskServiceTest {

  CallbackTaskRepository callbackTaskRepositoryMock;

  CallbackTaskService callbackTaskService;

  @Autowired
  CallbackTaskRepository callbackTaskRepository;

  @Autowired
  DiagnosisKeyBatchRepository diagnosisKeyBatchRepository;

  @Autowired
  CertificateRepository certificateRepository;

  @Autowired
  CallbackSubscriptionRepository callbackSubscriptionRepository;

  @Autowired
  DiagnosisKeyEntityRepository diagnosisKeyEntityRepository;

  @Autowired
  CallbackService callbackService;

  @Before
  public void setup() {
    callbackTaskRepository.deleteAll();
    callbackSubscriptionRepository.deleteAll();
    diagnosisKeyBatchRepository.deleteAll();
    certificateRepository.deleteAll();


    callbackTaskRepositoryMock = Mockito.mock(CallbackTaskRepository.class);
    callbackTaskService = new CallbackTaskService(callbackTaskRepositoryMock, callbackService);
  }

  @Test
  public void callbackTaskCleanUpShouldCallRepoMethod() {

    ZonedDateTime timestamp = ZonedDateTime.now();

    ArgumentCaptor<ZonedDateTime> captor = ArgumentCaptor.forClass(ZonedDateTime.class);

    callbackTaskService.removeTaskLocksOlderThan(timestamp);

    verify(callbackTaskRepositoryMock).removeTaskLocksOlderThan(captor.capture());
    Assert.assertEquals(timestamp, captor.getValue());

  }

  @Test
  public void notifiyAllCallbackSubscribersMethodShouldCreateACallbackTaskForEachSubscriber() {
    CallbackSubscriptionEntity callbackSubscription1 = createCallbackSubscriptionEntity(ZonedDateTime.now(), "r1");
    CallbackSubscriptionEntity callbackSubscription2 = createCallbackSubscriptionEntity(ZonedDateTime.now(), "r2");
    CallbackSubscriptionEntity callbackSubscription3 = createCallbackSubscriptionEntity(ZonedDateTime.now(), "r3");

    DiagnosisKeyBatchEntity batch = new DiagnosisKeyBatchEntity(null, ZonedDateTime.now(), "batchTag", null);
    batch = diagnosisKeyBatchRepository.save(batch);

    callbackTaskService.notifyAllCountriesForNewBatchTag(batch);

    List<CallbackTaskEntity> callbackTasks = callbackTaskRepository.findAll();

    ArgumentCaptor<CallbackTaskEntity> captor = ArgumentCaptor.forClass(CallbackTaskEntity.class);
    verify(callbackTaskRepositoryMock, times(3)).save(captor.capture());

    Assert.assertEquals(3, captor.getAllValues().size());
    Assert.assertEquals(callbackSubscription1.getId(), captor.getAllValues().get(0).getCallbackSubscription().getId());
    Assert.assertEquals(callbackSubscription2.getId(), captor.getAllValues().get(1).getCallbackSubscription().getId());
    Assert.assertEquals(callbackSubscription3.getId(), captor.getAllValues().get(2).getCallbackSubscription().getId());
  }

  private CallbackSubscriptionEntity createCallbackSubscriptionEntity(ZonedDateTime timestamp, String random) {
    DiagnosisKeyBatchEntity diagnosisKeyBatchEntity = new DiagnosisKeyBatchEntity(null, ZonedDateTime.now(), "batch", "link");
    diagnosisKeyBatchEntity = diagnosisKeyBatchRepository.save(diagnosisKeyBatchEntity);

    CertificateEntity certificateEntity = new CertificateEntity(null, ZonedDateTime.now(), random, "DE", CertificateEntity.CertificateType.CALLBACK, false);
    certificateEntity = certificateRepository.save(certificateEntity);

    CallbackSubscriptionEntity callbackSubscriptionEntity = new CallbackSubscriptionEntity(null, random, ZonedDateTime.now(), "url", "DE", certificateEntity);
    return callbackSubscriptionRepository.save(callbackSubscriptionEntity);
  }

}
