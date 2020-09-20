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

package eu.interop.federationgateway.batchsigning;

import com.google.protobuf.ByteString;
import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.model.EfgsProto.DiagnosisKey;
import eu.interop.federationgateway.model.EfgsProto.DiagnosisKeyBatch;
import io.netty.handler.codec.base64.Base64;
import javassist.bytecode.ByteArray;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.hibernate.loader.BatchLoadSizingStrategy;
import org.junit.Assert;
import org.junit.Test;

public class BatchSignatureUtilsTest {

  private static final int REPORT_TYPE = 1;
  private static final long MAX_UINT_VALUE = 4294967295L;

  static DiagnosisKeyBatch createDiagnosisKeyBatch(final List<String> keys) {
    final DiagnosisKeyBatch.Builder diagnosisKeyBatch = DiagnosisKeyBatch.newBuilder();
    for (String key : keys) {
      diagnosisKeyBatch.addKeys(createDiagnosisKey(key));
    }
    return diagnosisKeyBatch.build();
  }

  private static DiagnosisKey createDiagnosisKey(final String key) {
    DiagnosisKey.Builder diagnosisKey = DiagnosisKey.newBuilder();
  
    if(key==null|| key=="INVALID")
    {
      byte[] keyData= new byte[16];
      Random byteRandom = new Random();
      byteRandom.nextBytes(keyData);
      if(key=="INVALID") //Fill array with invalid UTF8 bytes
      {
          while(ByteString.copyFrom(keyData).isValidUtf8())
          {
            byteRandom.nextBytes(keyData);
          }
      }
      diagnosisKey.setKeyData(ByteString.copyFrom(keyData));
    }else
      diagnosisKey.setKeyData(ByteString.copyFrom(key.getBytes()));
 
    diagnosisKey.setRollingStartIntervalNumber(TestData.ROLLING_START_INTERVAL_NUMBER);
    diagnosisKey.setRollingPeriod(TestData.ROLLING_PERIOD);
    diagnosisKey.setTransmissionRiskLevel(TestData.TRANSMISSION_RISK_LEVEL);
    diagnosisKey.addAllVisitedCountries(TestData.VISITED_COUNTRIES_LIST);
    diagnosisKey.setOrigin(TestData.AUTH_CERT_COUNTRY);
    diagnosisKey.setReportTypeValue(REPORT_TYPE);
    diagnosisKey.setDaysSinceOnsetOfSymptoms(TestData.DAYS_SINCE_ONSET_OF_SYMPTOMS);
    return diagnosisKey.build();
  }

  public static byte[] createBytesToSignForDummyKey(final DiagnosisKey diagnosisKey)
  {
    final ByteArrayOutputStream batchBytes = new ByteArrayOutputStream();

    batchBytes.writeBytes(diagnosisKey.getKeyData().toByteArray());
    batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
    batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(diagnosisKey.getRollingStartIntervalNumber()).array());
    batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
    batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(diagnosisKey.getRollingPeriod()).array());
    batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
    batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(diagnosisKey.getTransmissionRiskLevel()).array());
    batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
    diagnosisKey.getVisitedCountriesList().forEach(country -> {
      batchBytes.writeBytes(country.getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
    });

    batchBytes.writeBytes(diagnosisKey.getOrigin().getBytes(StandardCharsets.UTF_8));
    batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
    batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(diagnosisKey.getReportTypeValue()).array());
    batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
    batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(diagnosisKey.getDaysSinceOnsetOfSymptoms()).array());

    return batchBytes.toByteArray();
  }

  public static byte[] createBytesToSign(final DiagnosisKeyBatch batch) {
    final ByteArrayOutputStream batchBytes = new ByteArrayOutputStream();
    final List<DiagnosisKey> sortedBatch = sortBatchByKeyData(batch);
    for (DiagnosisKey diagnosisKey : sortedBatch) {
      batchBytes.writeBytes(createBytesToSignForDummyKey(diagnosisKey));
    }
    return batchBytes.toByteArray();
  }

  public static byte[] createBytesToSignForDummyBatch(final DiagnosisKeyBatch batch) {
    final ByteArrayOutputStream batchBytes = new ByteArrayOutputStream();
    final List<DiagnosisKey> sortedBatch = sortBatchByKeyData(batch);
    for (DiagnosisKey diagnosisKey : sortedBatch) {
      batchBytes.writeBytes(diagnosisKey.getKeyData().toByteArray()); // 1 - KeyData
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(TestData.ROLLING_START_INTERVAL_NUMBER).array()); // 2 - rollingStartIntervalNumber
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(TestData.ROLLING_PERIOD).array()); // 3 - rollingPeriod
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(TestData.TRANSMISSION_RISK_LEVEL).array()); // 4 - transmissionRiskLevel
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(TestData.COUNTRY_A.getBytes(StandardCharsets.UTF_8)); // 5 - visitedCountries
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(TestData.COUNTRY_B.getBytes(StandardCharsets.UTF_8)); // 5 - visitedCountries
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(TestData.COUNTRY_C.getBytes(StandardCharsets.UTF_8)); // 5 - visitedCountries
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(TestData.COUNTRY_D.getBytes(StandardCharsets.UTF_8)); // 5 - visitedCountries
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(TestData.AUTH_CERT_COUNTRY.getBytes(StandardCharsets.UTF_8)); // 6 - origin
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(REPORT_TYPE).array()); // 7 - ReportType
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(TestData.DAYS_SINCE_ONSET_OF_SYMPTOMS).array());
    }
    return batchBytes.toByteArray();
  }

  private static List<DiagnosisKey> sortBatchByKeyData(final DiagnosisKeyBatch batch) {
    return batch.getKeysList()
      .stream()
      .sorted(Comparator.comparing(diagnosisKey -> java.util.Base64.getEncoder().encodeToString(diagnosisKey.getKeyData().toByteArray())))
      .collect(Collectors.toList());
  }

  static byte[] createBytesToSignWithIncorrectOrder(final DiagnosisKeyBatch batch) {
    final ByteArrayOutputStream batchBytes = new ByteArrayOutputStream();
    final List<DiagnosisKey> sortedBatch = sortBatchByKeyData(batch);
    for (DiagnosisKey diagnosisKey : sortedBatch) {
      batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(TestData.ROLLING_START_INTERVAL_NUMBER).array());
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(diagnosisKey.getKeyData().toByteArray());
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(TestData.TRANSMISSION_RISK_LEVEL).array());
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(TestData.ROLLING_PERIOD).array());
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(TestData.COUNTRY_A.getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(TestData.COUNTRY_B.getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(TestData.COUNTRY_C.getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(TestData.COUNTRY_D.getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(ByteBuffer.allocate(4).putInt(REPORT_TYPE).array());
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(TestData.AUTH_CERT_COUNTRY.getBytes(StandardCharsets.UTF_8));
      batchBytes.writeBytes(".".getBytes(StandardCharsets.UTF_8));
    }
    return batchBytes.toByteArray();
  }

  @Test
  public void testGetBytesToVerify() {
    final DiagnosisKeyBatch batch = createDiagnosisKeyBatch(List.of("123"));
    final byte[] bytesToVerify = BatchSignatureUtils.generateBytesToVerify(batch);
    final byte[] expectedBytes = createBytesToSignForDummyBatch(batch);
    Assert.assertNotNull(bytesToVerify);
    Assert.assertEquals(expectedBytes.length, bytesToVerify.length);
    Assert.assertArrayEquals(expectedBytes, bytesToVerify);
  }

  @Test
  public void testGetBytesToVerifyWhenBatchContainsSeveralDiagnosisKeys() {
    final DiagnosisKeyBatch batch = createDiagnosisKeyBatch(List.of("978", "D189", "ABC", "123"));
    final byte[] bytesToVerify = BatchSignatureUtils.generateBytesToVerify(batch);
    final byte[] expectedBytes = createBytesToSignForDummyBatch(batch);
    Assert.assertNotNull(bytesToVerify);
    Assert.assertEquals(expectedBytes.length, bytesToVerify.length);
    Assert.assertArrayEquals(expectedBytes, bytesToVerify);
  }

  @Test
  public void testMaxUintValue() throws IOException {
    final DiagnosisKeyBatch batch = DiagnosisKeyBatch.parseFrom(readBatchFile("diagnosisKeyBatchMaxValUint.bin"));
    final byte[] bytesToVerify = BatchSignatureUtils.generateBytesToVerify(batch);
    Assert.assertNotNull(bytesToVerify);

    final byte[] rollingStartIntervalNumber = {bytesToVerify[4], bytesToVerify[5], bytesToVerify[6], bytesToVerify[7]};
    Assert.assertEquals(ByteBuffer.wrap(rollingStartIntervalNumber).getInt() & 0xffffffffL, MAX_UINT_VALUE);

    final byte[] rollingPeriod = {bytesToVerify[9], bytesToVerify[10], bytesToVerify[11], bytesToVerify[12]};
    Assert.assertEquals(ByteBuffer.wrap(rollingPeriod).getInt() & 0xffffffffL, MAX_UINT_VALUE);
  }

  @Test
  public void testMaxIntValue() throws IOException {
    final DiagnosisKeyBatch batch = DiagnosisKeyBatch.parseFrom(readBatchFile("diagnosisKeyBatchMaxValInt.bin"));
    final byte[] bytesToVerify = BatchSignatureUtils.generateBytesToVerify(batch);
    Assert.assertNotNull(bytesToVerify);

    final byte[] rollingStartIntervalNumber = {bytesToVerify[4], bytesToVerify[5], bytesToVerify[6], bytesToVerify[7]};
    Assert.assertEquals(ByteBuffer.wrap(rollingStartIntervalNumber).getInt(), Integer.MAX_VALUE);

    final byte[] rollingPeriod = {bytesToVerify[9], bytesToVerify[10], bytesToVerify[11], bytesToVerify[12]};
    Assert.assertEquals(ByteBuffer.wrap(rollingPeriod).getInt(), Integer.MAX_VALUE);
  }

  @Test
  public void testSmallIntValue() throws IOException {
    final DiagnosisKeyBatch batch = DiagnosisKeyBatch.parseFrom(readBatchFile("diagnosisKeyBatchSmallInt.bin"));
    final byte[] bytesToVerify = BatchSignatureUtils.generateBytesToVerify(batch);
    Assert.assertNotNull(bytesToVerify);

    final byte[] rollingStartIntervalNumber = {bytesToVerify[4], bytesToVerify[5], bytesToVerify[6], bytesToVerify[7]};
    Assert.assertEquals(ByteBuffer.wrap(rollingStartIntervalNumber).getInt(), 5);

    final byte[] rollingPeriod = {bytesToVerify[9], bytesToVerify[10], bytesToVerify[11], bytesToVerify[12]};
    Assert.assertEquals(ByteBuffer.wrap(rollingPeriod).getInt(), 5);
  }

  private InputStream readBatchFile(final String filename) {
    return getClass().getClassLoader().getResourceAsStream(filename);
  }



  @Test
  public void testRandomKeySignatures()
  {
    for (int x=0; x<100000;x++) {
      DiagnosisKey key = createDiagnosisKey(null);

      var bytesToVerify = BatchSignatureUtils.generateBytesToVerify(key);
      var expectedBytes = createBytesToSignForDummyKey(key);

      Assert.assertNotNull(bytesToVerify);
      Assert.assertEquals(expectedBytes.length, bytesToVerify.length);
      Assert.assertArrayEquals(expectedBytes, bytesToVerify);

    }
    
  }

}
