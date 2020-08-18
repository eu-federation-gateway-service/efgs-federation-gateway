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
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.repository.CallbackSubscriptionRepository;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
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
public class CallbackServiceTest {

  @Autowired
  CallbackSubscriptionRepository callbackSubscriptionRepository;

  @Autowired
  EfgsProperties efgsProperties;

  CertificateService certificateServiceMock;

  CallbackService callbackService;

  @Before
  public void setUp() {
    certificateServiceMock = Mockito.mock(CertificateService.class);
    callbackService = new CallbackService(callbackSubscriptionRepository, efgsProperties, certificateServiceMock);
  }

  @Test
  public void testCheckUrlMethod() {
    Mockito.when(certificateServiceMock.getCallbackCertificateForHost("example.org", TestData.COUNTRY_A)).thenReturn(
      Optional.of(new CertificateEntity(42L, ZonedDateTime.now(), "thumb",
        TestData.COUNTRY_A, CertificateEntity.CertificateType.CALLBACK, false, "example.org")));

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

    Mockito.when(certificateServiceMock.getCallbackCertificateForHost("example.org", TestData.COUNTRY_A)).thenReturn(
      Optional.empty()
    );

    // check that a certificate is present
    Assert.assertFalse(callbackService.checkUrl("https://example.org", TestData.COUNTRY_A));

    try {
      InetAddress.getByName("example.org");
      // check that url's hostname is resolved
      Assert.assertTrue(callbackService.checkUrl("https://example.org", TestData.COUNTRY_A));
    } catch (UnknownHostException ignored) {
    } // skipping positive test case if no name resolution is possible

    Mockito.when(certificateServiceMock.getCallbackCertificateForHost("example.org", TestData.COUNTRY_A)).thenReturn(
      Optional.of(new CertificateEntity(42L, ZonedDateTime.now(), "thumb",
        TestData.COUNTRY_A, CertificateEntity.CertificateType.CALLBACK, true, "example.org")));

    // check that a certificate is not revoked
    Assert.assertFalse(callbackService.checkUrl("https://example.org", TestData.COUNTRY_A));
  }


}
