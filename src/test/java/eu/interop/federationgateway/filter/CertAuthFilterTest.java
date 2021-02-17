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

package eu.interop.federationgateway.filter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.repository.CertificateRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyBatchRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import eu.interop.federationgateway.testconfig.EfgsTestKeyStore;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = EfgsTestKeyStore.class)
public class CertAuthFilterTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private EfgsProperties properties;

  @Autowired
  private DiagnosisKeyEntityRepository diagnosisKeyEntityRepository;

  @Autowired
  private DiagnosisKeyBatchRepository diagnosisKeyBatchRepository;

  @Autowired
  private CertificateAuthentificationFilter certFilter;

  @Autowired
  private CertificateRepository certificateRepository;

  private MockMvc mockMvc;

  @Before
  public void setup() throws CertificateException, NoSuchAlgorithmException, IOException, OperatorCreationException, InvalidKeyException, SignatureException, KeyStoreException {
    TestData.insertCertificatesForAuthentication(certificateRepository);

    diagnosisKeyEntityRepository.deleteAll();
    diagnosisKeyBatchRepository.deleteAll();

    mockMvc = MockMvcBuilders
      .webAppContextSetup(context)
      .addFilter(certFilter)
      .build();
  }

  @Test()
  public void testRequestShouldFailIfDNHeaderIsMissing() throws Exception {
    mockMvc.perform(get("/diagnosiskeys/download/s")
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
    ).andExpect(status().isForbidden());
  }

  @Test
  public void testRequestShouldFailIfThumbprintHeaderIsMissing() throws Exception {
    mockMvc.perform(get("/diagnosiskeys/download/s")
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    ).andExpect(status().isForbidden());
  }

  @Test
  public void testRequestShouldFailIfCertHeadersAreMissing() throws Exception {
    mockMvc.perform(get("/diagnosiskeys/download/s")
      .accept("application/protobuf; version=1.0")
    ).andExpect(status().isForbidden());
  }

  @Test
  public void testRequestShouldFailIfCertIsNotOnWhitelist() throws Exception {
    mockMvc.perform(get("/diagnosiskeys/download/s")
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), "randomString")
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    ).andExpect(status().isForbidden());
  }

  @Test
  public void testFilterShouldAppendCountryAndThumbprintToRequestObject() throws Exception {
    mockMvc.perform(get("/diagnosiskeys/download/s")
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    ).andExpect(mvcResult -> {
      Assert.assertEquals("DE", mvcResult.getRequest().getAttribute(CertificateAuthentificationFilter.REQUEST_PROP_COUNTRY));
      Assert.assertEquals(
        TestData.AUTH_CERT_HASH,
        mvcResult.getRequest().getAttribute(CertificateAuthentificationFilter.REQUEST_PROP_THUMBPRINT)
      );
    });
  }

  @Test
  public void testFilterShouldDecodeDnString() throws Exception {
    String encodedDnString = "ST%3dSome-State%2c%20C%3dDE%2c%20O%3dInternet%20Widgits%20Pty%20Ltd%2c%20CN%3dTest%20Cert";

    mockMvc.perform(get("/diagnosiskeys/download/s")
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), encodedDnString)
    ).andExpect(mvcResult -> {
      Assert.assertEquals("DE", mvcResult.getRequest().getAttribute(CertificateAuthentificationFilter.REQUEST_PROP_COUNTRY));
      Assert.assertEquals(
        TestData.AUTH_CERT_HASH,
        mvcResult.getRequest().getAttribute(CertificateAuthentificationFilter.REQUEST_PROP_THUMBPRINT)
      );
    });
  }

  @Test
  public void testFilterShouldDecodeBase64AndUrlEncodedCertThumbprint() throws Exception {
    byte[] certHashBytes = new BigInteger(TestData.AUTH_CERT_HASH, 16).toByteArray();

    if (certHashBytes[0] == 0) {
      byte[] truncatedCertHashBytes = new byte[certHashBytes.length - 1];
      System.arraycopy(certHashBytes, 1, truncatedCertHashBytes, 0, truncatedCertHashBytes.length);
      certHashBytes = truncatedCertHashBytes;
    }

    String encodedThumbprint =
      URLEncoder.encode(Base64.getEncoder().encodeToString(certHashBytes), StandardCharsets.UTF_8);

    mockMvc.perform(get("/diagnosiskeys/download/s")
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), encodedThumbprint)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), "O=Test Firma GmbH,C=DE,U=,TR,TT=43")
    ).andExpect(mvcResult -> {
      Assert.assertEquals("DE", mvcResult.getRequest().getAttribute(CertificateAuthentificationFilter.REQUEST_PROP_COUNTRY));
      Assert.assertEquals(
        TestData.AUTH_CERT_HASH,
        mvcResult.getRequest().getAttribute(CertificateAuthentificationFilter.REQUEST_PROP_THUMBPRINT)
      );
    });
  }

  @Test
  public void testFilterShouldDecodeBase64EncodedCertThumbprint() throws Exception {
    byte[] certHashBytes = new BigInteger(TestData.AUTH_CERT_HASH, 16).toByteArray();

    if (certHashBytes[0] == 0) {
      byte[] truncatedCertHashBytes = new byte[certHashBytes.length - 1];
      System.arraycopy(certHashBytes, 1, truncatedCertHashBytes, 0, truncatedCertHashBytes.length);
      certHashBytes = truncatedCertHashBytes;
    }

    String encodedThumbprint = Base64.getEncoder().encodeToString(certHashBytes);

    mockMvc.perform(get("/diagnosiskeys/download/s")
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), encodedThumbprint)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), "O=Test Firma GmbH,C=DE,U=,TR,TT=43")
    ).andExpect(mvcResult -> {
      Assert.assertEquals("DE", mvcResult.getRequest().getAttribute(CertificateAuthentificationFilter.REQUEST_PROP_COUNTRY));
      Assert.assertEquals(
        TestData.AUTH_CERT_HASH,
        mvcResult.getRequest().getAttribute(CertificateAuthentificationFilter.REQUEST_PROP_THUMBPRINT)
      );
    });
  }


  @Test
  public void testRequestShouldFailIfCountryIsNotPresentInDnString() throws Exception {
    mockMvc.perform(get("/diagnosiskeys/download/s")
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), "O=Test Firma GmbH,U=Abteilung XYZ,TR=test")
    ).andExpect(status().isBadRequest());
  }

  @Test
  public void testFilterShouldFindCountryEvenOnMalformedDnString() throws Exception {
    mockMvc.perform(get("/diagnosiskeys/download/s")
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), "O=Test Firma GmbH,C=DE,U=,TR,TT=43")
    ).andExpect(mvcResult -> {
      Assert.assertEquals("DE", mvcResult.getRequest().getAttribute(CertificateAuthentificationFilter.REQUEST_PROP_COUNTRY));
    });
  }

  @Test
  public void testRequestShouldNotFailIfDnStringContainsDuplicatedKeys() throws Exception {
    mockMvc.perform(get("/diagnosiskeys/download/s")
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), "O=Test Firma GmbH,O=XXX,C=DE,U=Abteilung XYZ,TR=test")
    ).andExpect(mvcResult -> {
      Assert.assertEquals("DE", mvcResult.getRequest().getAttribute(CertificateAuthentificationFilter.REQUEST_PROP_COUNTRY));
    });
  }
}
