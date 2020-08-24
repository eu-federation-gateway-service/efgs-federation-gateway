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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.googlecode.protobuf.format.ProtobufFormatter;
import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.batchsigning.BatchSignatureUtilsTest;
import eu.interop.federationgateway.batchsigning.SignatureGenerator;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.config.ProtobufConverter;
import eu.interop.federationgateway.filter.CertificateAuthentificationFilter;
import eu.interop.federationgateway.model.AuditEntry;
import eu.interop.federationgateway.model.EfgsProto;
import eu.interop.federationgateway.repository.CertificateRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyBatchRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import eu.interop.federationgateway.service.DiagnosisKeyBatchService;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class AuditControllerTest {

  private final ObjectMapper mapper = new ObjectMapper();
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private EfgsProperties properties;
  @Autowired
  private DiagnosisKeyEntityRepository diagnosisKeyEntityRepository;
  @Autowired
  private CertificateRepository certificateRepository;
  @Autowired
  private CertificateAuthentificationFilter certFilter;
  @Autowired
  private DiagnosisKeyBatchService diagnosisKeyBatchService;
  @Autowired
  private DiagnosisKeyBatchRepository diagnosisKeyBatchRepository;
  private MockMvc mockMvc;
  private SignatureGenerator signatureGenerator;

  private static String getDateString(LocalDateTime timestamp) {
    return timestamp.format(DateTimeFormatter.ISO_DATE);
  }

  @Before
  public void setup() throws NoSuchAlgorithmException, CertificateException, CertIOException, OperatorCreationException {
    TestData.insertCertificatesForAuthentication(certificateRepository);
    signatureGenerator = new SignatureGenerator(certificateRepository);

    diagnosisKeyBatchRepository.deleteAll();
    diagnosisKeyEntityRepository.deleteAll();

    mockMvc = MockMvcBuilders
      .webAppContextSetup(context)
      .addFilter(certFilter)
      .build();
  }

  @Test
  public void testGetAuditInformation() throws Exception {
    ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    String formattedDate = currentDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String batchTag = formattedDate + "-1";

    String batchSignature = createDiagnosisKeysTestData();
    MvcResult mvcResult = mockMvc.perform(get("/diagnosiskeys/audit/download/" + batchTag)
      .accept(MediaType.APPLICATION_JSON_VALUE)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isOk())
      .andReturn();

    String jsonResult = mvcResult.getResponse().getContentAsString();
    mapper.registerModule(new JavaTimeModule());
    List<AuditEntry> auditEntries = mapper.readValue(jsonResult, new TypeReference<>() {
    });

    Assert.assertEquals(1, auditEntries.size());
    AuditEntry auditEntry = auditEntries.get(0);
    Assert.assertEquals("DE", auditEntry.getCountry());
    Assert.assertEquals(3, auditEntry.getAmount());
    Assert.assertEquals("69c697c045b4cdaa441a28af0ec1cc4128153b9ddc796b66bfa04b02ea3e103e",
      auditEntry.getUploaderThumbprint());
    Assert.assertEquals(batchSignature, auditEntry.getBatchSignature());
  }

  @Test
  public void testRequestShouldFailIfBatchTagDoesNotExists() throws Exception {
    mockMvc.perform(get("/diagnosiskeys/audit/download/" + TestData.SECOND_BATCHTAG)
      .accept(MediaType.APPLICATION_JSON_VALUE)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isNotFound());
  }

  private String createDiagnosisKeysTestData() throws Exception {
    EfgsProto.DiagnosisKey key1 = TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(10).build();
    EfgsProto.DiagnosisKey key2 = TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(20).build();
    EfgsProto.DiagnosisKey key3 = TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(30).build();

    EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder().addAllKeys(Arrays.asList(key1,
      key2, key3)).build();

    byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch);
    String signature = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    ProtobufFormatter formatter = new ProtobufConverter();
    String jsonFormatted = formatter.printToString(batch);

    log.info("Json Formatted Payload: {}", jsonFormatted);

    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", signature)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(batch.toByteArray()))
      .andExpect(status().isCreated());

    diagnosisKeyBatchService.batchDocuments();
    return signature;
  }
}
