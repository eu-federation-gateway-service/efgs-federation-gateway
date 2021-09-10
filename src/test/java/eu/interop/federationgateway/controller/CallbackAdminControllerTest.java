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

package eu.interop.federationgateway.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.CallbackSubscriptionEntity;
import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.filter.CertificateAuthentificationFilter;
import eu.interop.federationgateway.model.Callback;
import eu.interop.federationgateway.repository.CallbackSubscriptionRepository;
import eu.interop.federationgateway.repository.CertificateRepository;
import eu.interop.federationgateway.service.CallbackService;
import eu.interop.federationgateway.testconfig.EfgsTestKeyStore;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@SpringBootTest
@ContextConfiguration(classes = EfgsTestKeyStore.class)
public class CallbackAdminControllerTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private EfgsProperties properties;

  @Autowired
  private CertificateRepository certificateRepository;

  @Autowired
  private CertificateAuthentificationFilter certFilter;

  @Autowired
  private CallbackService callbackService;

  @Autowired
  private CallbackSubscriptionRepository callbackSubscriptionRepository;

  private MockMvc mockMvc;

  private CertificateEntity callbackCert;

  private CertificateEntity callbackCert2;

  private boolean dnsIsAvailable = false;

  @BeforeEach
  public void setup() throws NoSuchAlgorithmException, CertificateException, IOException,
    OperatorCreationException, InvalidKeyException, SignatureException, KeyStoreException {

    try {
      InetAddress.getByName("example.org");
      // check that url's hostname is resolved
      dnsIsAvailable = true;
    } catch (UnknownHostException ignored) {
      dnsIsAvailable = false;
    } // skipping positive test case if no name resolution is possible


    callbackSubscriptionRepository.deleteAll();
    TestData.insertCertificatesForAuthentication(certificateRepository);

    callbackCert = certificateRepository.save(new CertificateEntity(
      null, ZonedDateTime.now(ZoneOffset.UTC), "xxx",
      "DE", CertificateEntity.CertificateType.CALLBACK, false,
      new URL(TestData.CALLBACK_URL_EFGS).getHost(),
      null,
      null
    ));

    callbackCert2 = certificateRepository.save(new CertificateEntity(
      null, ZonedDateTime.now(ZoneOffset.UTC), "xxx2",
      "DE", CertificateEntity.CertificateType.CALLBACK, false,
      new URL(TestData.CALLBACK_URL_EXAMPLE).getHost(),
      null,
      null
    ));

    mockMvc = MockMvcBuilders
      .webAppContextSetup(context)
      .addFilter(certFilter)
      .build();
  }

  @AfterEach
  public void teardown() {
    callbackSubscriptionRepository.deleteAll();
    certificateRepository.delete(callbackCert);
    certificateRepository.delete(callbackCert2);
  }

  @Test
  public void testPutCallback() throws Exception {
    Assumptions.assumeTrue(dnsIsAvailable);

    String firstId = TestData.CALLBACK_ID_FIRST;

    mockMvc.perform(put("/diagnosiskeys/callback/" + firstId)
      .param("url", TestData.CALLBACK_URL_EXAMPLE)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk());

    Assertions.assertEquals(1, callbackSubscriptionRepository.count());
    CallbackSubscriptionEntity callbackSubscriptionEntity =
      callbackSubscriptionRepository.findByCallbackIdAndCountryIs(firstId, TestData.AUTH_CERT_COUNTRY).get();
    Assertions.assertEquals(firstId, callbackSubscriptionEntity.getCallbackId());
    Assertions.assertEquals(TestData.AUTH_CERT_COUNTRY, callbackSubscriptionEntity.getCountry());
    Assertions.assertEquals(TestData.CALLBACK_URL_EXAMPLE, callbackSubscriptionEntity.getUrl());
  }

  @Test
  public void testSubscribeCallbackWithInvalidUrl() throws Exception {

    String firstId = TestData.CALLBACK_ID_FIRST;

    mockMvc.perform(put("/diagnosiskeys/callback/" + firstId)
      .param("url", "http://google.com")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isBadRequest());

    Assertions.assertEquals(0, callbackSubscriptionRepository.count());

    mockMvc.perform(put("/diagnosiskeys/callback/" + firstId)
      .param("url", "https://localhost")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isBadRequest());

    Assertions.assertEquals(0, callbackSubscriptionRepository.count());

    mockMvc.perform(put("/diagnosiskeys/callback/" + firstId)
      .param("url", "https://192.168.178.58")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isBadRequest());

    Assertions.assertEquals(0, callbackSubscriptionRepository.count());

    mockMvc.perform(put("/diagnosiskeys/callback/" + firstId)
      .param("url", "https://notify.me/?you=evil_sql_injection")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isBadRequest());

    Assertions.assertEquals(0, callbackSubscriptionRepository.count());
  }

  @Test
  public void testPutCallbackAndChangeUrlForExistingCallback() throws Exception {
    Assumptions.assumeTrue(dnsIsAvailable);

    String firstId = TestData.CALLBACK_ID_FIRST;

    mockMvc.perform(put("/diagnosiskeys/callback/" + firstId)
      .param("url", TestData.CALLBACK_URL_EXAMPLE)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk());

    CallbackSubscriptionEntity callbackSubscription = callbackSubscriptionRepository.findByCallbackIdAndCountryIs(firstId, TestData.AUTH_CERT_COUNTRY).get();
    Assertions.assertEquals(TestData.CALLBACK_URL_EXAMPLE, callbackSubscription.getUrl());

    mockMvc.perform(put("/diagnosiskeys/callback/" + firstId)
      .param("url", TestData.CALLBACK_URL_EFGS)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk());

    Assertions.assertEquals(1, callbackSubscriptionRepository.count());
    callbackSubscription = callbackSubscriptionRepository.findByCallbackIdAndCountryIs(firstId, TestData.AUTH_CERT_COUNTRY).get();
    Assertions.assertEquals(firstId, callbackSubscription.getCallbackId());
    Assertions.assertEquals("DE", callbackSubscription.getCountry());
    Assertions.assertEquals(TestData.CALLBACK_URL_EFGS, callbackSubscription.getUrl());
  }

  @Test
  public void testDeleteCallback() throws Exception {
    Assumptions.assumeTrue(dnsIsAvailable);

    String secondId = TestData.CALLBACK_ID_SECOND;

    mockMvc.perform(put("/diagnosiskeys/callback/" + secondId)
      .param("url", TestData.CALLBACK_URL_EXAMPLE)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk());
    Assertions.assertEquals(1, callbackSubscriptionRepository.count());

    mockMvc.perform(delete("/diagnosiskeys/callback/" + secondId)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk());

    Assertions.assertEquals(0, callbackSubscriptionRepository.count());
  }

  @Test
  public void testDeleteCallbackByUnknownId() throws Exception {
    String unknownId = "unknownId";

    mockMvc.perform(delete("/diagnosiskeys/callback/" + unknownId)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isNotFound());
  }

  @Test
  public void testGetAllCallbacksForACountry() throws Exception {
    String firstId = TestData.CALLBACK_ID_FIRST;
    String secondId = TestData.CALLBACK_ID_SECOND;

    callbackService.saveCallbackSubscription(new CallbackSubscriptionEntity(
      1L, ZonedDateTime.now(ZoneOffset.UTC), firstId, TestData.CALLBACK_URL_EXAMPLE, TestData.AUTH_CERT_COUNTRY)
    );

    callbackService.saveCallbackSubscription(new CallbackSubscriptionEntity(
      2L, ZonedDateTime.now(ZoneOffset.UTC), secondId, TestData.CALLBACK_URL_EXAMPLE, TestData.AUTH_CERT_COUNTRY)
    );

    callbackService.saveCallbackSubscription(new CallbackSubscriptionEntity(
      3L, ZonedDateTime.now(ZoneOffset.UTC), firstId, TestData.CALLBACK_URL_EXAMPLE, TestData.COUNTRY_A)
    );


    mockMvc.perform(get("/diagnosiskeys/callback")
      .accept("application/json; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk())
      .andExpect(mvcResult -> {
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        List<Callback> callbacks = mapper.readValue(jsonResponse, new TypeReference<>() {
        });
        Assertions.assertEquals(2, callbacks.size());
        Assertions.assertEquals(firstId, callbacks.get(0).getCallbackId());
        Assertions.assertEquals(TestData.CALLBACK_URL_EXAMPLE, callbacks.get(0).getUrl());

        Assertions.assertEquals(secondId, callbacks.get(1).getCallbackId());
        Assertions.assertEquals(TestData.CALLBACK_URL_EXAMPLE, callbacks.get(1).getUrl());
      });
  }
}
