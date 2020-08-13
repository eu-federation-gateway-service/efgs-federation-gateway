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

package eu.interop.federationgateway.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.CallbackSubscriptionEntity;
import eu.interop.federationgateway.filter.CertificateAuthentificationFilter;
import eu.interop.federationgateway.model.Callback;
import eu.interop.federationgateway.repository.CallbackSubscriptionRepository;
import eu.interop.federationgateway.repository.CertificateRepository;
import eu.interop.federationgateway.service.CallbackService;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
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

  @Before
  public void setup() throws NoSuchAlgorithmException, CertificateException, CertIOException,
    OperatorCreationException {

    callbackSubscriptionRepository.deleteAll();
    TestData.insertCertificatesForAuthentication(certificateRepository);

    mockMvc = MockMvcBuilders
      .webAppContextSetup(context)
      .addFilter(certFilter)
      .build();
  }

  @After
  public void teardown() {
    callbackSubscriptionRepository.deleteAll();
  }

  @Test
  public void testPutCallback() throws Exception {

    String firstId = TestData.CALLBACK_ID_FIRST;

    mockMvc.perform(put("/diagnosiskeys/callback/" + firstId)
      .contentType("text/plain; version=1.0")
      .header("url", TestData.CALLBACK_URL_EXAMPLE)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk());

    Assert.assertEquals(1, callbackSubscriptionRepository.count());
    CallbackSubscriptionEntity callbackSubscriptionEntity =
      callbackSubscriptionRepository.findByCallbackId(firstId).get();
    Assert.assertEquals(firstId, callbackSubscriptionEntity.getCallbackId());
    Assert.assertEquals("DE", callbackSubscriptionEntity.getCountry());
    Assert.assertEquals(TestData.CALLBACK_URL_EXAMPLE, callbackSubscriptionEntity.getUrl());
  }

  @Test
  public void testPutCallbackAndChangeUrlForExistingCallback() throws Exception {
    String firstId = TestData.CALLBACK_ID_FIRST;

    mockMvc.perform(put("/diagnosiskeys/callback/" + firstId)
      .contentType("text/plain; version=1.0")
      .header("url", TestData.CALLBACK_URL_EXAMPLE)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk());

    CallbackSubscriptionEntity callbackSubscription = callbackSubscriptionRepository.findByCallbackId(firstId).get();
    Assert.assertEquals(TestData.CALLBACK_URL_EXAMPLE, callbackSubscription.getUrl());

    mockMvc.perform(put("/diagnosiskeys/callback/" + firstId)
      .contentType("text/plain; version=1.0")
      .header("url", TestData.CALLBACK_URL_EFGS)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk());

    Assert.assertEquals(1, callbackSubscriptionRepository.count());
    callbackSubscription = callbackSubscriptionRepository.findByCallbackId(firstId).get();
    Assert.assertEquals(firstId, callbackSubscription.getCallbackId());
    Assert.assertEquals("DE", callbackSubscription.getCountry());
    Assert.assertEquals(TestData.CALLBACK_URL_EFGS, callbackSubscription.getUrl());
  }

  @Test
  public void testDeleteCallback() throws Exception {
    String secondId = TestData.CALLBACK_ID_SECOND;

    mockMvc.perform(put("/diagnosiskeys/callback/" + secondId)
      .contentType("text/plain; version=1.0")
      .header("url", TestData.CALLBACK_URL_EXAMPLE)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk());
    Assert.assertEquals(1, callbackSubscriptionRepository.count());

    mockMvc.perform(delete("/diagnosiskeys/callback/" + secondId)
      .contentType("text/plain; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk());

    Assert.assertEquals(0, callbackSubscriptionRepository.count());
    Optional<CallbackSubscriptionEntity> callbackSubscriptionEntity = callbackSubscriptionRepository.findByCallbackId(secondId);
    Assert.assertTrue(callbackSubscriptionEntity.isEmpty());
  }

  @Test
  public void testDeleteCallbackByUnknownId() throws Exception {
    String unknownId = "unknownId";

    mockMvc.perform(delete("/diagnosiskeys/callback/" + unknownId)
      .contentType("text/plain; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isNotFound());
  }

  @Test
  public void testGetAllCallbacksForAThumbprint() throws Exception {
    String firstId = TestData.CALLBACK_ID_FIRST;
    String secondId = TestData.CALLBACK_ID_SECOND;

    mockMvc.perform(put("/diagnosiskeys/callback/" + firstId)
      .contentType("text/plain; version=1.0")
      .header("url", TestData.CALLBACK_URL_EXAMPLE)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk());

    mockMvc.perform(put("/diagnosiskeys/callback/" + secondId)
      .contentType("text/plain; version=1.0")
      .header("url", TestData.CALLBACK_URL_EFGS)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk());


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
        Assert.assertEquals(2, callbacks.size());
        Callback callback = callbacks.get(0);
        Assert.assertEquals(firstId, callback.getCallbackId());
        Assert.assertEquals(TestData.CALLBACK_URL_EXAMPLE, callback.getUrl());
      });
  }
}
