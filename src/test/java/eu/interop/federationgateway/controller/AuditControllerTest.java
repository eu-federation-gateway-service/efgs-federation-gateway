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
import eu.interop.federationgateway.testconfig.EfgsTestKeyStore;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
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
@ContextConfiguration(classes = EfgsTestKeyStore.class)
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

  @Before
  public void setup() throws NoSuchAlgorithmException, CertificateException, IOException,
    OperatorCreationException, InvalidKeyException, SignatureException, KeyStoreException {
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
    MvcResult mvcResult =
      mockMvc.perform(get("/diagnosiskeys/audit/download/" + getDateString(currentDateTime) + "/" + batchTag)
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
    Assert.assertEquals(TestData.AUTH_CERT_HASH, auditEntry.getUploaderThumbprint());
    Assert.assertNotNull(auditEntry.getUploaderOperatorSignature());
    Assert.assertNotNull(auditEntry.getSigningCertificateOperatorSignature());
    Assert.assertNotNull(auditEntry.getUploaderCertificate());
    Assert.assertNotNull(auditEntry.getSigningCertificate());
    Assert.assertEquals(batchSignature, auditEntry.getBatchSignature());
  }

  @Test
  public void testGetAuditInformationByYesterdayUpload() throws Exception {
    ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    String formattedDate = currentDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String batchTag = formattedDate + "-1";

    String batchSignature = createDiagnosisKeysTestData();
    
    // change the key creation date to yesterday
    ZonedDateTime newCreationTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
    diagnosisKeyEntityRepository.findAll().forEach(key -> {
      key.setCreatedAt(newCreationTime);
        diagnosisKeyEntityRepository.save(key);
    });

    MvcResult mvcResult =
      mockMvc.perform(get("/diagnosiskeys/audit/download/" + getDateString(currentDateTime) + "/" + batchTag)
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
    Assert.assertEquals(TestData.AUTH_CERT_HASH, auditEntry.getUploaderThumbprint());
    Assert.assertNotNull(auditEntry.getUploaderOperatorSignature());
    Assert.assertNotNull(auditEntry.getSigningCertificateOperatorSignature());
    Assert.assertNotNull(auditEntry.getUploaderCertificate());
    Assert.assertNotNull(auditEntry.getSigningCertificate());
    Assert.assertEquals(batchSignature, auditEntry.getBatchSignature());
  }

  @Test
  public void testRequestShouldFailIfBatchTagDoesNotExists() throws Exception {
    ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    String formattedDate = currentDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String batchTag = formattedDate + "-1";

    mockMvc.perform(
      get("/diagnosiskeys/audit/download/" + getDateString(currentDateTime) + "/" + batchTag)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
        .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isNotFound());
  }

  @Test
  public void testRequestShouldFailIfNoEntityExistForDate() throws Exception {
    ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
    String formattedDate = currentDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String batchTag = formattedDate + "-1";
    createDiagnosisKeysTestData();
    mockMvc.perform(
      get("/diagnosiskeys/audit/download/" + getDateString(currentDateTime) + "/" + batchTag)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
        .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isNotFound());
  }

  @Test
  public void testRequestShouldFailIfDateExpired() throws Exception {
    ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(2);

    mockMvc.perform(
      get("/diagnosiskeys/audit/download/" + getDateString(currentDateTime) + "/" + TestData.SECOND_BATCHTAG)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
        .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE))
      .andExpect(status().isGone());
  }

  private String createDiagnosisKeysTestData() throws Exception {
    EfgsProto.DiagnosisKey key1 = buildKey(3);
    EfgsProto.DiagnosisKey key2 = buildKey(4);
    EfgsProto.DiagnosisKey key3 = buildKey(5);

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

  private EfgsProto.DiagnosisKey buildKey(int transmissionRiskLevel) {
    return TestData.getDiagnosisKeyProto().toBuilder()
      .setTransmissionRiskLevel(transmissionRiskLevel)
      .setDaysSinceOnsetOfSymptoms(1)
      .setRollingStartIntervalNumber(Math.toIntExact(Instant.now().getEpochSecond() / 600))
      .setRollingPeriod(1)
      .build();
  }

  private static String getDateString(ZonedDateTime timestamp) {
    return timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE);
  }
}
