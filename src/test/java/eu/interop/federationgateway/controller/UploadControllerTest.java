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

import com.google.protobuf.ByteString;
import com.googlecode.protobuf.format.ProtobufFormatter;
import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.batchsigning.BatchSignatureUtilsTest;
import eu.interop.federationgateway.batchsigning.SignatureGenerator;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.config.ProtobufConverter;
import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.filter.CertificateAuthentificationFilter;
import eu.interop.federationgateway.model.EfgsProto;
import eu.interop.federationgateway.repository.CertificateRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import eu.interop.federationgateway.service.CertificateService;
import eu.interop.federationgateway.testconfig.EfgsTestKeyStore;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = EfgsTestKeyStore.class)
public class UploadControllerTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private EfgsProperties properties;

  @Autowired
  private DiagnosisKeyEntityRepository diagnosisKeyEntityRepository;

  @Autowired
  private CertificateAuthentificationFilter certFilter;

  @Autowired
  private CertificateRepository certificateRepository;

  @Autowired
  private CertificateService certificateService;

  private SignatureGenerator signatureGenerator;

  private MockMvc mockMvc;

  private static final List<String> VISITED_COUNTRIES = Arrays.asList(
    "AA", "AB", "AC", "AD", "AE", "AF", "AG", "AH", "AI", "AJ", "AK", "AL", "AM",
    "AN", "AO", "AP", "AQ", "AR", "AS", "AT", "AU", "AV", "AW", "AX", "AY", "YZ",
    "BA", "BB", "BC", "BD", "BE", "BF", "BG", "BH", "BI", "BJ", "BK", "BL", "BM"
  );

  @Before
  public void setup() throws NoSuchAlgorithmException, CertificateException, IOException,
    OperatorCreationException, InvalidKeyException, SignatureException {
    signatureGenerator = new SignatureGenerator(certificateRepository);

    diagnosisKeyEntityRepository.deleteAll();
    mockMvc = MockMvcBuilders
      .webAppContextSetup(context)
      .addFilter(certFilter)
      .build();
  }

  @Test
  public void testRequestShouldFailOnMissingBatchSignature() throws Exception {
    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(new byte[1])
    ).andExpect(status().isBadRequest());
  }

  @Test
  public void testRequestShouldFailOnInvalidBatchSignature() throws Exception {
    EfgsProto.DiagnosisKey key1 = buildKey(1);
    EfgsProto.DiagnosisKey key2 = buildKey(2);
    EfgsProto.DiagnosisKey key3 = buildKey(3);

    EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Arrays.asList(key1, key2, key3)).build();

    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", "invalidSignature")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(batch.toByteArray())
    ).andExpect(status().isBadRequest());
  }

  @Test
  public void testRequestUploadKeysInProtobufFormat() throws Exception {
    EfgsProto.DiagnosisKey key1 = buildKey(1);
    EfgsProto.DiagnosisKey key2 = buildKey(2);
    EfgsProto.DiagnosisKey key3 = buildKey(3);

    EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Arrays.asList(key1, key2, key3)).build();

    byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch);
    String signature = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", signature)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(batch.toByteArray())
    )
      .andExpect(status().isCreated())
      .andExpect(result -> Assert.assertEquals(batch.getKeysCount(), diagnosisKeyEntityRepository.count()));
  }

  @Test
  public void testRequestUploadKeysInProtobufFormatWithModifiedCertDatabase() throws Exception {
    EfgsProto.DiagnosisKey key1 = buildKey(1, TestData.COUNTRY_A);
    EfgsProto.DiagnosisKey key2 = buildKey(2, TestData.COUNTRY_A);
    EfgsProto.DiagnosisKey key3 = buildKey(3, TestData.COUNTRY_A);

    EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Arrays.asList(key1, key2, key3)).build();

    byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch);
    String signature = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    CertificateEntity certificateEntity = certificateService.getCertificate(
      TestData.validCertificateHash, TestData.AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.SIGNING
    ).get();

    certificateEntity.setCountry(TestData.COUNTRY_A);

    certificateRepository.save(certificateEntity);

    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", signature)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(batch.toByteArray())
    )
      .andExpect(status().isBadRequest())
      .andExpect(result -> Assert.assertEquals(0, diagnosisKeyEntityRepository.count()));
  }

  @Test
  public void testRequestUploadKeysExistingHashInProtobufFormat() throws Exception {
    EfgsProto.DiagnosisKey key1 = buildKey(1);
    EfgsProto.DiagnosisKey key2 = buildKey(2);

    EfgsProto.DiagnosisKeyBatch batch1 = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Arrays.asList(key1)).build();

    byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch1);
    String signatureBatch1 = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    EfgsProto.DiagnosisKeyBatch batch2 = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Arrays.asList(key1, key2)).build();

    bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch2);
    String signatureBatch2 = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    // The first upload request should successfully insert key1 and report it as HTTP 201 (created).
    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", signatureBatch1)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(batch1.toByteArray())
    ).andExpect(status().isCreated());

    // The second upload request should report one conflict (key1 as 409) and
    // one insert (key2 as 201).
    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.SECOND_BATCHTAG)
      .header("batchSignature", signatureBatch2)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(batch2.toByteArray())
    )
      .andExpect(status().isMultiStatus())
      .andExpect(result -> {
        // According to the backend response(s) there should be two keys in the database now.
        Assert.assertEquals(2, diagnosisKeyEntityRepository.count());

        JsonParser jsonParser = JsonParserFactory.getJsonParser();
        Map<String, Object> map = jsonParser.parseMap(result.getResponse().getContentAsString());
        List<Integer> list201 = (List<Integer>) map.get("201");
        List<Integer> list409 = (List<Integer>) map.get("409");
        List<Integer> list500 = (List<Integer>) map.get("500");

        Assert.assertTrue(list500.isEmpty());
        Assert.assertTrue(list201.contains(1));
        Assert.assertTrue(list409.contains(0));
        Assert.assertEquals(1, list201.size());
        Assert.assertEquals(1, list409.size());
      });
  }

  @Test
  public void testRequestUploadKeysExistingBatchTag() throws Exception {
    EfgsProto.DiagnosisKey key1 = buildKey(1);
    EfgsProto.DiagnosisKey key2 = buildKey(2);

    EfgsProto.DiagnosisKeyBatch batch1 = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Collections.singletonList(key1)).build();

    byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch1);
    String signatureBatch1 = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    EfgsProto.DiagnosisKeyBatch batch2 = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Collections.singletonList(key2)).build();

    bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch2);
    String signatureBatch2 = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", signatureBatch1)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(batch1.toByteArray())
    ).andExpect(status().isCreated());

    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", signatureBatch2)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(batch2.toByteArray())
    )
      .andExpect(status().isConflict())
      .andExpect(result -> Assert.assertEquals(1, diagnosisKeyEntityRepository.count()));
  }

  @Test
  public void testRequestSizeMaximum() throws Exception {
    EfgsProto.DiagnosisKeyBatch.Builder batchBuilder = EfgsProto.DiagnosisKeyBatch.newBuilder();

    for (int i = 0; i < properties.getUploadSettings().getMaximumUploadBatchSize(); i++) {
      batchBuilder.addKeys(buildKey(ByteBuffer.allocate(16).putInt(i).array()));
    }

    EfgsProto.DiagnosisKeyBatch batch = batchBuilder.build();

    byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch);
    String signature = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", signature)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(batch.toByteArray())
    ).andExpect(status().isCreated());
  }

  @Test
  public void testRequestSizeTooLarge() throws Exception {
    EfgsProto.DiagnosisKeyBatch.Builder batchBuilder = EfgsProto.DiagnosisKeyBatch.newBuilder();

    for (int i = 0; i < properties.getUploadSettings().getMaximumUploadBatchSize() + 1; i++) {
      batchBuilder.addKeys(buildKey(ByteBuffer.allocate(16).putInt(i).array()));
    }

    EfgsProto.DiagnosisKeyBatch batch = batchBuilder.build();

    byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch);
    String signature = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", signature)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(batch.toByteArray())
    ).andExpect(status().isPayloadTooLarge());
  }

  @Test
  public void testRequestUploadKeysInJsonFormat() throws Exception {
    EfgsProto.DiagnosisKey key1 = buildKey(1);
    EfgsProto.DiagnosisKey key2 = buildKey(2);
    EfgsProto.DiagnosisKey key3 = buildKey(3);

    EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Arrays.asList(key1, key2, key3)).build();

    byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch);
    String signature = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    ProtobufFormatter formatter = new ProtobufConverter();
    String jsonFormatted = formatter.printToString(batch);

    Base64.Encoder base64Encoder = Base64.getEncoder();

    Assert.assertTrue(jsonFormatted.contains(base64Encoder.encodeToString(key1.getKeyData().toByteArray())));
    Assert.assertTrue(jsonFormatted.contains(base64Encoder.encodeToString(key2.getKeyData().toByteArray())));
    Assert.assertTrue(jsonFormatted.contains(base64Encoder.encodeToString(key3.getKeyData().toByteArray())));

    log.info("Json Formatted Payload: {}", jsonFormatted);

    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/json; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", signature)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(jsonFormatted)
    )
      .andExpect(status().isCreated())
      .andExpect(result -> Assert.assertEquals(batch.getKeysCount(), diagnosisKeyEntityRepository.count()));
  }

  @Test
  public void testRequestUploadKeysWithInvalidJson() throws Exception {
    EfgsProto.DiagnosisKey key1 = buildKey(1);

    EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Collections.singletonList(key1)).build();

    byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch);
    String signature = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    // Create JSON representation of diagnosiskey
    ProtobufFormatter formatter = new ProtobufConverter();
    String jsonFormatted = formatter.printToString(batch);

    // replace keyData with invalid base64
    jsonFormatted = jsonFormatted.replaceFirst("keyData\": \"[A-Za-z0-9=]*\"", "keyData\": \"invalidBase64\"");

    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/json; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", signature)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(jsonFormatted)
    )
      .andExpect(status().isBadRequest());
  }

  @Test
  public void testRequestUploadKeysInProtobufFormatFailedByTrustedAnchor() throws Exception {
    EfgsProto.DiagnosisKey key1 = buildKey(1);
    EfgsProto.DiagnosisKey key2 = buildKey(2);
    EfgsProto.DiagnosisKey key3 = buildKey(3);

    EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Arrays.asList(key1, key2, key3)).build();

    byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch);
    String signature = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", signature)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), "trashHash")
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(batch.toByteArray())
    )
      .andExpect(status().isForbidden());
  }

  private EfgsProto.DiagnosisKey buildKey(int transmissionRiskLevel, String origin) {
    return TestData.getDiagnosisKeyProto().toBuilder()
      .addAllVisitedCountries(VISITED_COUNTRIES)
      .setTransmissionRiskLevel(transmissionRiskLevel)
      .setOrigin(origin)
      .setDaysSinceOnsetOfSymptoms(1)
      .setRollingStartIntervalNumber(Math.toIntExact(Instant.now().getEpochSecond() / 600))
      .setRollingPeriod(1)
      .build();
  }

  private EfgsProto.DiagnosisKey buildKey(byte[] keyData) {
    return TestData.getDiagnosisKeyProto().toBuilder()
      .addAllVisitedCountries(VISITED_COUNTRIES)
      .setTransmissionRiskLevel(1)
      .setKeyData(ByteString.copyFrom(keyData))
      .setDaysSinceOnsetOfSymptoms(1)
      .setRollingStartIntervalNumber(Math.toIntExact(Instant.now().getEpochSecond() / 600))
      .setRollingPeriod(1)
      .build();
  }

  private EfgsProto.DiagnosisKey buildKey(int transmissionRiskLevel) {
    return TestData.getDiagnosisKeyProto().toBuilder()
      .addAllVisitedCountries(VISITED_COUNTRIES)
      .setTransmissionRiskLevel(transmissionRiskLevel)
      .setDaysSinceOnsetOfSymptoms(1)
      .setRollingStartIntervalNumber(Math.toIntExact(Instant.now().getEpochSecond() / 600))
      .setRollingPeriod(1)
      .build();
  }
}
