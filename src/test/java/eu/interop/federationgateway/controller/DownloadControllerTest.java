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

import com.googlecode.protobuf.format.ProtobufFormatter;
import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.config.ProtobufConverter;
import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.filter.CertificateAuthentificationFilter;
import eu.interop.federationgateway.model.EfgsProto;
import eu.interop.federationgateway.repository.CertificateRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyBatchRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import eu.interop.federationgateway.testconfig.EfgsTestKeyStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = EfgsTestKeyStore.class)
public class DownloadControllerTest {

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

  private static String getDateString(ZonedDateTime timestamp) {
    return timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  @Before
  public void setup() throws CertificateException, NoSuchAlgorithmException, IOException,
    OperatorCreationException, InvalidKeyException, SignatureException, KeyStoreException {
    TestData.insertCertificatesForAuthentication(certificateRepository);

    diagnosisKeyEntityRepository.deleteAll();
    diagnosisKeyBatchRepository.deleteAll();

    mockMvc = MockMvcBuilders
      .webAppContextSetup(context)
      .addFilter(certFilter)
      .build();
  }

  @Test
  public void testRequestShouldFailIfRequestedDateIsToOld() throws Exception {
    int maxAge = properties.getDownloadSettings().getMaxAgeInDays();
    ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).minusDays(maxAge + 1);

    mockMvc.perform(get("/diagnosiskeys/download/" + getDateString(timestamp))
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    ).andExpect(status().isGone());
  }

  @Test
  public void testRequestShouldFailIfBatchTagDoesNotExists() throws Exception {
    ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC);

    mockMvc.perform(get("/diagnosiskeys/download/" + getDateString(timestamp))
      .accept("application/protobuf; version=1.0")
      .header("batchTag", "xxx")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    ).andExpect(status().isNotFound());
  }

  @Test
  public void testRequestShouldFailIfBatchTagAndDateDoesNotMatch() throws Exception {
    ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).minusDays(5);
    ZonedDateTime timestamp2 = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
    String batchTag = "batchTag";

    diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(null, timestamp2, batchTag, null));

    mockMvc.perform(get("/diagnosiskeys/download/" + getDateString(timestamp))
      .accept("application/protobuf; version=1.0")
      .header("batchTag", batchTag)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    ).andExpect(status().isBadRequest());
  }

  @Test
  public void testRequestShouldReturnNextBatchTag() throws Exception {
    ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).minusDays(2);
    ZonedDateTime timestamp2 = ZonedDateTime.now(ZoneOffset.UTC).minusDays(2).plusHours(1);
    String batchTag1 = getDateString(timestamp) + "-14";
    String batchTag2 = getDateString(timestamp) + "-15";

    diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(null, timestamp, batchTag1, batchTag2));
    diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(null, timestamp2, batchTag2, null));

    mockMvc.perform(get("/diagnosiskeys/download/" + getDateString(timestamp))
      .accept("application/protobuf; version=1.0")
      .header("batchTag", batchTag1)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    )
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/protobuf; version=1.0"))
      .andExpect(header().string("batchTag", batchTag1))
      .andExpect(header().string("nextBatchTag", batchTag2));
  }

  @Test
  public void testRequestShouldReturnNextBatchTagNullIfNoFurtherBatchExists() throws Exception {
    ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).minusDays(2);
    String batchTag1 = getDateString(timestamp) + "-14";

    diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(null, timestamp, batchTag1, null));

    mockMvc.perform(get("/diagnosiskeys/download/" + getDateString(timestamp))
      .accept("application/protobuf; version=1.0")
      .header("batchTag", batchTag1)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    )
      .andExpect(status().isOk())
      .andExpect(header().string("batchTag", batchTag1))
      .andExpect(header().string("nextBatchTag", "null"));
  }

  @Test
  public void testRequestShouldReturnKeysInCorrectOrder() throws Exception {
    String origin1 = "o1";
    String origin2 = "o2";
    String origin3 = "o3";
    ZonedDateTime timestampBatchTag = ZonedDateTime.now(ZoneOffset.UTC);
    String batchTag = getDateString(timestampBatchTag) + "-14";

    diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(null, timestampBatchTag, batchTag, null));

    saveDiagnosisEntityToDb(batchTag, origin1);
    saveDiagnosisEntityToDb(batchTag, origin2);
    saveDiagnosisEntityToDb(batchTag, origin3);

    mockMvc.perform(get("/diagnosiskeys/download/" + getDateString(timestampBatchTag))
      .accept("application/protobuf; version=1.0")
      .header("batchTag", batchTag)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    )
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/protobuf; version=1.0"))
      .andExpect(mvcResult -> {
        EfgsProto.DiagnosisKeyBatch response = EfgsProto.DiagnosisKeyBatch.parseFrom(
          mvcResult.getResponse().getContentAsByteArray());

        Assert.assertEquals(origin1, response.getKeys(0).getOrigin());
        Assert.assertEquals(origin2, response.getKeys(1).getOrigin());
        Assert.assertEquals(origin3, response.getKeys(2).getOrigin());
      });
  }

  @Test
  public void testRequestShouldReturnKeysAssignedToBatch() throws Exception {
    String origin1 = "o1";
    String origin2 = "o2";
    String origin3 = "o3";
    String origin4 = "o4";
    ZonedDateTime timestampBatchTag = ZonedDateTime.now(ZoneOffset.UTC).minusHours(2);
    ZonedDateTime timestampBatchTag2 = ZonedDateTime.now(ZoneOffset.UTC).minusHours(1);
    String batchTag = getDateString(timestampBatchTag) + "-14";
    String batchTag2 = getDateString(timestampBatchTag) + "-15";

    diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(null, timestampBatchTag, batchTag, batchTag2));
    diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(null, timestampBatchTag2, batchTag2, null));

    saveDiagnosisEntityToDb(batchTag, origin1);
    saveDiagnosisEntityToDb(batchTag, origin2);
    saveDiagnosisEntityToDb(batchTag2, origin3);
    saveDiagnosisEntityToDb(batchTag2, origin4);

    mockMvc.perform(get("/diagnosiskeys/download/" + getDateString(timestampBatchTag))
      .accept("application/protobuf; version=1.0")
      .header("batchTag", batchTag)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    )
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/protobuf; version=1.0"))
      .andExpect(mvcResult -> {
        EfgsProto.DiagnosisKeyBatch response = EfgsProto.DiagnosisKeyBatch.parseFrom(
          mvcResult.getResponse().getContentAsByteArray());

        Assert.assertEquals(origin1, response.getKeys(0).getOrigin());
        Assert.assertEquals(origin2, response.getKeys(1).getOrigin());
        Assert.assertEquals(2, response.getKeysCount());
      });
  }

  @Test
  public void testRequestShouldReturnFirstBatchOfTheDayIfNoBatchTagIsProvided() throws Exception {
    String origin1 = "o1";
    String origin2 = "o2";
    String origin3 = "o3";
    String origin4 = "o4";
    String origin5 = "o5";
    String origin6 = "o6";
    ZonedDateTime timestampBatchTag = ZonedDateTime.now(ZoneOffset.UTC).minusDays(2);
    ZonedDateTime timestampBatchTag2 = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1).minusHours(1);
    ZonedDateTime timestampBatchTag3 = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1).minusMinutes(30);
    String batchTag = getDateString(timestampBatchTag) + "-15";
    String batchTag2 = getDateString(timestampBatchTag) + "-1";
    String batchTag3 = getDateString(timestampBatchTag) + "-2";

    diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(null, timestampBatchTag, batchTag, null));
    diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(null, timestampBatchTag2, batchTag2, batchTag3));
    diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(null, timestampBatchTag3, batchTag3, null));

    saveDiagnosisEntityToDb(batchTag, origin1);
    saveDiagnosisEntityToDb(batchTag, origin2);
    saveDiagnosisEntityToDb(batchTag2, origin3);
    saveDiagnosisEntityToDb(batchTag2, origin4);
    saveDiagnosisEntityToDb(batchTag3, origin5);
    saveDiagnosisEntityToDb(batchTag3, origin6);

    mockMvc.perform(get("/diagnosiskeys/download/" + getDateString(timestampBatchTag2))
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    )
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/protobuf; version=1.0"))
      .andExpect(header().string("batchTag", batchTag2))
      .andExpect(header().string("nextBatchTag", batchTag3))
      .andExpect(mvcResult -> {
        EfgsProto.DiagnosisKeyBatch response = EfgsProto.DiagnosisKeyBatch.parseFrom(
          mvcResult.getResponse().getContentAsByteArray());

        Assert.assertEquals(origin3, response.getKeys(0).getOrigin());
        Assert.assertEquals(origin4, response.getKeys(1).getOrigin());
        Assert.assertEquals(2, response.getKeysCount());
      });
  }

  @Test
  public void testRequestShouldNotReturnKeysFromOwnCountry() throws Exception {
    String origin1 = "o1";
    String origin2 = "o2";
    String origin3 = "o3";
    String origin4 = "o4";
    String ownCountry = "DE";
    String otherCountry = "DK";
    ZonedDateTime timestampBatchTag = ZonedDateTime.now(ZoneOffset.UTC).minusDays(2);
    String batchTag = getDateString(timestampBatchTag) + "-1";
    diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(null, timestampBatchTag, batchTag, null));

    saveDiagnosisEntityToDb(batchTag, origin1, ownCountry);
    saveDiagnosisEntityToDb(batchTag, origin2, ownCountry);
    saveDiagnosisEntityToDb(batchTag, origin3, otherCountry);
    saveDiagnosisEntityToDb(batchTag, origin4, otherCountry);

    mockMvc.perform(get("/diagnosiskeys/download/" + getDateString(timestampBatchTag))
      .accept("application/protobuf; version=1.0")
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    )
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/protobuf; version=1.0"))
      .andExpect(mvcResult -> {
        EfgsProto.DiagnosisKeyBatch response = EfgsProto.DiagnosisKeyBatch.parseFrom(
          mvcResult.getResponse().getContentAsByteArray());

        Assert.assertEquals(origin3, response.getKeys(0).getOrigin());
        Assert.assertEquals(origin4, response.getKeys(1).getOrigin());
        Assert.assertEquals(2, response.getKeysCount());
      });
  }

  @Test
  public void testRequestShouldReturnKeysInJsonFormat() throws Exception {
    String origin1 = "o1";
    String origin2 = "o2";
    ZonedDateTime timestampBatchTag = ZonedDateTime.now(ZoneOffset.UTC).minusHours(2);
    String batchTag = getDateString(timestampBatchTag) + "-14";

    diagnosisKeyBatchRepository.save(new DiagnosisKeyBatchEntity(null, timestampBatchTag, batchTag, null));

    saveDiagnosisEntityToDb(batchTag, origin1);
    saveDiagnosisEntityToDb(batchTag, origin2);

    mockMvc.perform(get("/diagnosiskeys/download/" + getDateString(timestampBatchTag))
      .accept("application/json; version=1.0")
      .header("batchTag", batchTag)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
    )
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json; version=1.0"))
      .andExpect(mvcResult -> {
        Assert.assertTrue(
          mvcResult.getResponse().getContentAsString().contains(Base64.getEncoder().encodeToString(TestData.BYTES)));

        ProtobufFormatter formatter = new ProtobufConverter();
        EfgsProto.DiagnosisKeyBatch.Builder builder = EfgsProto.DiagnosisKeyBatch.newBuilder();

        formatter.merge(
          new ByteArrayInputStream(mvcResult.getResponse().getContentAsByteArray()),
          StandardCharsets.UTF_8,
          builder
        );

        EfgsProto.DiagnosisKeyBatch response = builder.build();

        Assert.assertEquals(origin1, response.getKeys(0).getOrigin());
        Assert.assertEquals(origin2, response.getKeys(1).getOrigin());
        Assert.assertEquals(2, response.getKeysCount());
      });
  }

  private void saveDiagnosisEntityToDb(String batchTag, String origin) {
    saveDiagnosisEntityToDb(batchTag, origin, null);
  }

  private void saveDiagnosisEntityToDb(String batchTag, String origin, String country) {
    DiagnosisKeyEntity entity = TestData.getDiagnosisKeyTestEntityforCreation();
    entity.setBatchTag(batchTag);
    entity.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));

    if (country != null) {
      entity.getUploader().setCountry(country);
    }

    entity.getPayload().setOrigin(origin);
    entity.setPayloadHash(UUID.randomUUID().toString());
    diagnosisKeyEntityRepository.save(entity);
  }

}
