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

package eu.interop.federationgateway.filter;

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.repository.CertificateRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyBatchRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
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
  public void setup() throws CertificateException, NoSuchAlgorithmException, CertIOException, OperatorCreationException {
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
  public void testFilterShouldDecodeCertThumbprint() throws Exception {
    String encodedThumbprint = "69%3AC6%3A97%3Ac0%3A45%3Ab4%3Acd%3Aaa%3A44%3A1a%3A28%3A"
      + "AF%3A0e%3Ac1%3Acc%3A41%3A28%3A15%3A3B%3A9d%3Adc%3A79%3A6b%3A66%3Abf%3Aa0%3A4b%3A02%3AEa%3A3e%3A10%3A3e";

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
}
