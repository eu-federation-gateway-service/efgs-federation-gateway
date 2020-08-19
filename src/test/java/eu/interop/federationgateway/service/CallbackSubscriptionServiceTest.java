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

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.entity.CallbackSubscriptionEntity;
import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.repository.CallbackSubscriptionRepository;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CallbackSubscriptionServiceTest {

  @Autowired
  CallbackSubscriptionRepository callbackSubscriptionRepository;

  CertificateService certificateServiceMock;

  CallbackSubscriptionService callbackSubscriptionService;

  @Before
  public void setUp() {
    certificateServiceMock = Mockito.mock(CertificateService.class);
    callbackSubscriptionService = new CallbackSubscriptionService(callbackSubscriptionRepository, certificateServiceMock);
  }

  @Test
  public void testSaveMethodUpdatesExistingEntries() {
    CallbackSubscriptionEntity entity = new CallbackSubscriptionEntity(
      null, null, TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EXAMPLE, TestData.COUNTRY_A);

    entity = callbackSubscriptionService.saveCallbackSubscription(entity);

    Assert.assertEquals(1, callbackSubscriptionRepository.count());

    CallbackSubscriptionEntity newEntity = new CallbackSubscriptionEntity(
      null, null, TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EFGS, TestData.COUNTRY_A);

    newEntity = callbackSubscriptionService.saveCallbackSubscription(newEntity);

    Assert.assertEquals(1, callbackSubscriptionRepository.count());
    Assert.assertEquals(TestData.CALLBACK_URL_EFGS, callbackSubscriptionRepository.findAll().get(0).getUrl());
    Assert.assertEquals(entity.getId(), newEntity.getId());

    CallbackSubscriptionEntity entityOtherCountry = new CallbackSubscriptionEntity(
      null, null, TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EFGS, TestData.COUNTRY_B);

    callbackSubscriptionService.saveCallbackSubscription(entityOtherCountry);

    Assert.assertEquals(2, callbackSubscriptionRepository.count());
  }

  @Test
  public void testDeleteCallbackSubscription() {
    CallbackSubscriptionEntity entity = new CallbackSubscriptionEntity(
      null, null, TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EXAMPLE, TestData.COUNTRY_A);

    entity = callbackSubscriptionService.saveCallbackSubscription(entity);

    Assert.assertEquals(1, callbackSubscriptionRepository.count());

    callbackSubscriptionService.deleteCallbackSubscription(entity);

    Assert.assertEquals(0, callbackSubscriptionRepository.count());
  }

  @Test
  public void testGetAllCallbackSubscription() {
    CallbackSubscriptionEntity[] given = {
      new CallbackSubscriptionEntity(
        null, null, TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EXAMPLE, TestData.COUNTRY_A),
      new CallbackSubscriptionEntity(
        null, null, TestData.CALLBACK_ID_SECOND, TestData.CALLBACK_URL_EXAMPLE, TestData.COUNTRY_A),
      new CallbackSubscriptionEntity(
        null, null, TestData.CALLBACK_ID_FIRST, TestData.CALLBACK_URL_EXAMPLE, TestData.COUNTRY_B)
    };

    callbackSubscriptionRepository.saveAll(List.of(given));
    Assert.assertEquals(3, callbackSubscriptionRepository.count());

    List<CallbackSubscriptionEntity> entities = callbackSubscriptionService.getAllCallbackSubscriptions();
    Assert.assertEquals(3, entities.size());
    Assert.assertArrayEquals(given, entities.toArray());

    entities = callbackSubscriptionService.getAllCallbackSubscriptionsForCountry(TestData.COUNTRY_A);
    Assert.assertEquals(2, entities.size());
    Assert.assertArrayEquals(new CallbackSubscriptionEntity[]{ given[0], given[1] }, entities.toArray());

    entities = callbackSubscriptionService.getAllCallbackSubscriptionsForCountry(TestData.COUNTRY_B);
    Assert.assertEquals(1, entities.size());
    Assert.assertArrayEquals(new CallbackSubscriptionEntity[]{ given[2] }, entities.toArray());
  }

  @Test
  public void testCheckUrlMethod() {
    Mockito.when(certificateServiceMock.getCallbackCertificateForHost("example.org", TestData.COUNTRY_A)).thenReturn(
      Optional.of(new CertificateEntity(42L, ZonedDateTime.now(), "thumb",
        TestData.COUNTRY_A, CertificateEntity.CertificateType.CALLBACK, false, "example.org")));

    // check if given string is a url
    Assert.assertFalse(callbackSubscriptionService.checkUrl("teststring1234", TestData.COUNTRY_A));

    // check for correct protocol (https)
    Assert.assertFalse(callbackSubscriptionService.checkUrl("http://example.org", TestData.COUNTRY_A));

    // check that url has no query parameters
    Assert.assertFalse(callbackSubscriptionService.checkUrl("https://example.org?abc=123", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackSubscriptionService.checkUrl("https://localhost", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackSubscriptionService.checkUrl("https://127.0.0.1", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackSubscriptionService.checkUrl("https://10.2.5.6", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackSubscriptionService.checkUrl("https://100.64.23.5", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackSubscriptionService.checkUrl("https://169.254.85.69", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackSubscriptionService.checkUrl("https://172.16.5.3", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackSubscriptionService.checkUrl("https://192.168.178.5", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackSubscriptionService.checkUrl("https://::1", TestData.COUNTRY_A));

    // check that url is not a local target
    Assert.assertFalse(callbackSubscriptionService.checkUrl("https://fd9e:35ga:352e:1212::1", TestData.COUNTRY_A));

    Mockito.when(certificateServiceMock.getCallbackCertificateForHost("example.org", TestData.COUNTRY_A)).thenReturn(
      Optional.empty()
    );

    // check that a certificate is present
    Assert.assertFalse(callbackSubscriptionService.checkUrl("https://example.org", TestData.COUNTRY_A));

    try {
      InetAddress.getByName("example.org");
      // check that url's hostname is resolved
      Assert.assertTrue(callbackSubscriptionService.checkUrl("https://example.org", TestData.COUNTRY_A));
    } catch (UnknownHostException ignored) {
    } // skipping positive test case if no name resolution is possible

    Mockito.when(certificateServiceMock.getCallbackCertificateForHost("example.org", TestData.COUNTRY_A)).thenReturn(
      Optional.of(new CertificateEntity(42L, ZonedDateTime.now(), "thumb",
        TestData.COUNTRY_A, CertificateEntity.CertificateType.CALLBACK, true, "example.org")));

    // check that a certificate is not revoked
    Assert.assertFalse(callbackSubscriptionService.checkUrl("https://example.org", TestData.COUNTRY_A));
  }


}
