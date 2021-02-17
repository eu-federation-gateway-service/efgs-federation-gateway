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

package eu.interop.federationgateway.batchsigning;

import com.google.protobuf.ByteString;
import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.model.EfgsProto.DiagnosisKey;
import eu.interop.federationgateway.model.EfgsProto.DiagnosisKeyBatch;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
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

    if (key == null || key == "INVALID") {
      byte[] keyData = new byte[16];
      Random byteRandom = new Random();
      byteRandom.nextBytes(keyData);
      if (key == "INVALID") //Fill array with invalid UTF8 bytes
      {
        while (ByteString.copyFrom(keyData).isValidUtf8()) {
          byteRandom.nextBytes(keyData);
        }
      }
      diagnosisKey.setKeyData(ByteString.copyFrom(keyData));
    } else
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

  public static byte[] createBytesToSignForDummyKey(final DiagnosisKey diagnosisKey) {
    final ByteArrayOutputStream batchBytes = new ByteArrayOutputStream();

    batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(diagnosisKey.getKeyData().toByteArray()).getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(diagnosisKey.getRollingStartIntervalNumber()).array()).getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(diagnosisKey.getRollingPeriod()).array()).getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(diagnosisKey.getTransmissionRiskLevel()).array()).getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(String.join(",", diagnosisKey.getVisitedCountriesList()).getBytes(StandardCharsets.US_ASCII)).getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(diagnosisKey.getOrigin().getBytes(StandardCharsets.US_ASCII)).getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(diagnosisKey.getReportTypeValue()).array()).getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(diagnosisKey.getDaysSinceOnsetOfSymptoms()).array()).getBytes(StandardCharsets.US_ASCII));
    batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));

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
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(diagnosisKey.getKeyData().toByteArray()).getBytes(StandardCharsets.US_ASCII)); // 1 - KeyData
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(TestData.ROLLING_START_INTERVAL_NUMBER).array()).getBytes(StandardCharsets.US_ASCII)); // 2 - rollingStartIntervalNumber
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(TestData.ROLLING_PERIOD).array()).getBytes(StandardCharsets.US_ASCII));
      // 3 - rollingPeriod
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(TestData.TRANSMISSION_RISK_LEVEL).array()).getBytes(StandardCharsets.US_ASCII));// 4 - transmissionRiskLevel
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(String.join(",", List.of(TestData.COUNTRY_A, TestData.COUNTRY_B, TestData.COUNTRY_C, TestData.COUNTRY_D))
        .getBytes(StandardCharsets.US_ASCII)).getBytes(StandardCharsets.US_ASCII)); //5 - Visited Countries
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(TestData.AUTH_CERT_COUNTRY.getBytes(StandardCharsets.US_ASCII)).getBytes(StandardCharsets.US_ASCII));
      // 6 - origin
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(REPORT_TYPE).array()).getBytes(StandardCharsets.US_ASCII)); // 7 - ReportType
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(TestData.DAYS_SINCE_ONSET_OF_SYMPTOMS).array()).getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
    }
    return batchBytes.toByteArray();
  }

  private static List<DiagnosisKey> sortBatchByKeyData(final DiagnosisKeyBatch batch) {
    return batch.getKeysList()
      .stream()
      .sorted(Comparator.comparing(diagnosisKey -> java.util.Base64.getEncoder().encodeToString(BatchSignatureUtils.generateBytesToVerify(diagnosisKey))))
      .collect(Collectors.toList());
  }

  static byte[] createBytesToSignWithIncorrectOrder(final DiagnosisKeyBatch batch) {
    final ByteArrayOutputStream batchBytes = new ByteArrayOutputStream();
    final List<DiagnosisKey> sortedBatch = sortBatchByKeyData(batch);
    for (DiagnosisKey diagnosisKey : sortedBatch) {
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(TestData.ROLLING_START_INTERVAL_NUMBER).array()).getBytes(StandardCharsets.US_ASCII));
      
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(diagnosisKey.getKeyData().toByteArray()).getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(TestData.TRANSMISSION_RISK_LEVEL).array()).getBytes(StandardCharsets.US_ASCII));
      
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(TestData.ROLLING_PERIOD).array()).getBytes(StandardCharsets.US_ASCII));
      
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(String.join(",", List.of(TestData.COUNTRY_A, TestData.COUNTRY_B, TestData.COUNTRY_C, TestData.COUNTRY_D))
        .getBytes(StandardCharsets.US_ASCII)).getBytes(StandardCharsets.US_ASCII));

      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(ByteBuffer.allocate(4).putInt(REPORT_TYPE).array()).getBytes(StandardCharsets.US_ASCII));
      
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
      batchBytes.writeBytes(BatchSignatureUtils.bytesToBase64(TestData.AUTH_CERT_COUNTRY.getBytes(StandardCharsets.US_ASCII)).getBytes(StandardCharsets.US_ASCII));
      
      batchBytes.writeBytes(".".getBytes(StandardCharsets.US_ASCII));
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

    final byte[] rollingStartIntervalNumber = {bytesToVerify[5], bytesToVerify[6], bytesToVerify[7], bytesToVerify[8], bytesToVerify[9], bytesToVerify[10], bytesToVerify[11], bytesToVerify[12]};

    Assert.assertEquals(MAX_UINT_VALUE, ByteBuffer.wrap(BatchSignatureUtils.b64ToBytes(rollingStartIntervalNumber)).getInt() & 0xffffffffL);

    final byte[] rollingPeriod = {bytesToVerify[14], bytesToVerify[15], bytesToVerify[16], bytesToVerify[17], bytesToVerify[18], bytesToVerify[19], bytesToVerify[20], bytesToVerify[21]};
    Assert.assertEquals(MAX_UINT_VALUE, ByteBuffer.wrap(BatchSignatureUtils.b64ToBytes(rollingPeriod)).getInt() & 0xffffffffL);
  }

  @Test
  public void testMaxIntValue() throws IOException {
    final DiagnosisKeyBatch batch = DiagnosisKeyBatch.parseFrom(readBatchFile("diagnosisKeyBatchMaxValInt.bin"));
    final byte[] bytesToVerify = BatchSignatureUtils.generateBytesToVerify(batch);
    Assert.assertNotNull(bytesToVerify);

    final byte[] rollingStartIntervalNumber = {bytesToVerify[5], bytesToVerify[6], bytesToVerify[7], bytesToVerify[8], bytesToVerify[9], bytesToVerify[10], bytesToVerify[11], bytesToVerify[12]};
    Assert.assertEquals(Integer.MAX_VALUE, ByteBuffer.wrap(BatchSignatureUtils.b64ToBytes(rollingStartIntervalNumber)).getInt());

    final byte[] rollingPeriod = {bytesToVerify[14], bytesToVerify[15], bytesToVerify[16], bytesToVerify[17], bytesToVerify[18], bytesToVerify[19], bytesToVerify[20], bytesToVerify[21]};
    Assert.assertEquals(Integer.MAX_VALUE, ByteBuffer.wrap(BatchSignatureUtils.b64ToBytes(rollingPeriod)).getInt());
  }

  @Test
  public void testSmallIntValue() throws IOException {
    final DiagnosisKeyBatch batch = DiagnosisKeyBatch.parseFrom(readBatchFile("diagnosisKeyBatchSmallInt.bin"));
    final byte[] bytesToVerify = BatchSignatureUtils.generateBytesToVerify(batch);
    Assert.assertNotNull(bytesToVerify);

    final byte[] rollingStartIntervalNumber = {bytesToVerify[5], bytesToVerify[6], bytesToVerify[7], bytesToVerify[8], bytesToVerify[9], bytesToVerify[10], bytesToVerify[11], bytesToVerify[12]};
    Assert.assertEquals(5, ByteBuffer.wrap(BatchSignatureUtils.b64ToBytes(rollingStartIntervalNumber)).getInt());

    final byte[] rollingPeriod = {bytesToVerify[14], bytesToVerify[15], bytesToVerify[16], bytesToVerify[17], bytesToVerify[18], bytesToVerify[19], bytesToVerify[20], bytesToVerify[21]};
    Assert.assertEquals(5, ByteBuffer.wrap(BatchSignatureUtils.b64ToBytes(rollingPeriod)).getInt());
  }

  private InputStream readBatchFile(final String filename) {
    return getClass().getClassLoader().getResourceAsStream(filename);
  }

  @Test
  public void testRandomKeySignatures() {
    for (int x = 0; x < 100000; x++) {
      DiagnosisKey key = createDiagnosisKey(null);

      var bytesToVerify = BatchSignatureUtils.generateBytesToVerify(key);
      var expectedBytes = createBytesToSignForDummyKey(key);

      Assert.assertNotNull(bytesToVerify);
      Assert.assertEquals(expectedBytes.length, bytesToVerify.length);
      Assert.assertArrayEquals(expectedBytes, bytesToVerify);
    }
  }
  
  // CC 24/08/2020:
  // A test to show that currently, data uploaded to the EFGS can be manipulated without invalidating
  // the corresponding signature of the uploader. This means that signature verification currently does not
  // provide integrity.
  //
  // The underlying problem is that `generateBytesToVerify` does not provide an unambiguous encoding, and
  // glues arbitrary-length strings together without length indication. There are at least three variable-length aspects:
  //   - the length of the diagnosis key string after encoding,
  //   - the number of visited countries, and
  //   - the length of each country string.
  // Because lengths are not encoded and separations unclear, given a key batch, it is possible to construct a second
  // key batch that will have lead to the same output of `generateBytesToVerify` and will therefore also verify using
  // the signature of the original batch. 
  //
  // The test is only a very specific instance -- it is easy to change `generateBytesToVerify` such that this test 
  // passes, but without solving the underlying problem.
  //
  // Issue identified by Johannes Krupp @ CISPA,
  // Proof-of-concept/test by Cas Cremers @ CISPA.
  @Test
  public void testSignatureMalleability() {
    // Create two key batches, each with a single key. They have different keydata and other parameters.
    final DiagnosisKeyBatch batchOriginal = TestData.createDiagnosisKeyBatchDetails("1", 0x32330000, 0, 0x000000000, List.of("AA", "DE", "NL"));
    final DiagnosisKeyBatch batchModified = TestData.createDiagnosisKeyBatchDetails("123", 0x00000000, 0, 0x000004141, List.of("DE", "NL"));

    // Create the corresponding bytes for signing/verifying the signatre
    final byte[] bytesToVerifyOriginal = BatchSignatureUtils.generateBytesToVerify(batchOriginal);
    final byte[] bytesToVerifyModified = BatchSignatureUtils.generateBytesToVerify(batchModified);

    Assert.assertFalse(Arrays.equals(bytesToVerifyOriginal, bytesToVerifyModified));
  }

  //Issue/test by by ebeigarts
  //https://gist.github.com/ebeigarts/c868ae6ccbd51cc8d99bd456c4f8c61f
  @Test
  public void testInvalidCodePoints() {
    // "\xDD\xC7,\xA7\xFE\xCC\xFE\x99姽\x80\xE3\xD3\xCBy"
    byte[] k1 = new byte[]{-35, -57, 44, -89, -2, -52, -2, -103, -27, -89, -67, -128, -29, -45, -53, 121};
    // "\xD0\xC9,\xF7\xFE\xCC\xFE\x99姽\x80\xE3\xD3\xCBy"
    byte[] k2 = new byte[]{-48, -55, 44, -9, -2, -52, -2, -103, -27, -89, -67, -128, -29, -45, -53, 121};

    byte[] k3 = new byte[]{41, 42, 43};

    String k1s = new String(k1, 0, 16, StandardCharsets.UTF_8);
    String k2s = new String(k2, 0, 16, StandardCharsets.UTF_8);

    String k3s = new String(k3, 0, 3, StandardCharsets.UTF_8);

    Arrays.equals(k1, k2);

    Arrays.equals(k1s.getBytes(StandardCharsets.UTF_8), k2s.getBytes(StandardCharsets.UTF_8));

    var key1 = TestData.createDiagnosisKeyDetails(k1s, 233, 2, 2, List.of("DE"));
    var key2 = TestData.createDiagnosisKeyDetails(k2s, 233, 2, 2, List.of("DE"));
    var key3 = TestData.createDiagnosisKeyDetails(k3s, 233, 2, 2, List.of("DE"));

    var key1b = BatchSignatureUtils.generateBytesToVerify(key1);
    var key2b = BatchSignatureUtils.generateBytesToVerify(key2);
    var key3b = BatchSignatureUtils.generateBytesToVerify(key3);

    Assert.assertTrue(Arrays.equals(key1b, key2b));  //String Conversion with invalid codepoints to UTF8 returns the same

    var key11 = TestData.createDiagnosisKeyDetails(k1, 233, 2, 2, List.of("DE"));
    var key22 = TestData.createDiagnosisKeyDetails(k2, 233, 2, 2, List.of("DE"));
    var key33 = TestData.createDiagnosisKeyDetails(k3, 233, 2, 2, List.of("DE"));


    var key11b = BatchSignatureUtils.generateBytesToVerify(key11);
    var key22b = BatchSignatureUtils.generateBytesToVerify(key22);
    var key33b = BatchSignatureUtils.generateBytesToVerify(key33);

    Assert.assertFalse(Arrays.equals(key11b, key22b)); //Without UTF8 everything correct
    Assert.assertFalse(Arrays.equals(key11b, key1b));
    Assert.assertFalse(Arrays.equals(key22b, key2b));
    Assert.assertTrue(Arrays.equals(key3b, key33b)); //Always the same with valid characters
  }


  private byte[] ConvertArrayToByteArray(String[] list) {
    byte[] tmp = new byte[list.length];
    for (int x = 0; x < list.length; x++) {
      tmp[x] = (byte) Integer.parseInt(list[x]);
    }
    return tmp;
  }

  @Test
  public void testCSharpCrossLanguageCalculation()
    throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    File file = new File(classLoader.getResource("CSharpTestdata.txt").getFile());
    var path = file.getAbsolutePath().replace("%20", " ");
    BufferedReader br = new BufferedReader(new FileReader(path));

    List<DiagnosisKey> keys = new ArrayList<>();

    while (br.ready()) {
      String line = br.readLine();
      var parts = line.split("\\|");

      var diagnosiskeydata = parts[0].substring(1);
      diagnosiskeydata = diagnosiskeydata.substring(0, diagnosiskeydata.length() - 1);
      var diagnosiskeybytes = ConvertArrayToByteArray(diagnosiskeydata.split(","));
      var javadiagnosiskeyb64Result = BatchSignatureUtils.bytesToBase64(diagnosiskeybytes);
      var diagnosiskeyb64 = parts[1];

      Assert.assertEquals(javadiagnosiskeyb64Result, diagnosiskeyb64);

      var rollingStartIntervalNumber = parts[2].substring(1);
      rollingStartIntervalNumber = rollingStartIntervalNumber.substring(0, rollingStartIntervalNumber.length() - 1);
      var rollingStartIntervalNumberbytes = ConvertArrayToByteArray(rollingStartIntervalNumber.split(","));
      var javaRSIb64Result = BatchSignatureUtils.bytesToBase64(rollingStartIntervalNumberbytes);
      var rollingStartIntervalNumberb64 = parts[3];

      Assert.assertEquals(javaRSIb64Result, rollingStartIntervalNumberb64);

      var rollingPeriod = parts[4].substring(1);
      rollingPeriod = rollingPeriod.substring(0, rollingPeriod.length() - 1);
      var rollingPeriodbytes = ConvertArrayToByteArray(rollingPeriod.split(","));
      var javaRPb64Result = BatchSignatureUtils.bytesToBase64(rollingPeriodbytes);
      var rollingPeriodb64 = parts[5];

      Assert.assertEquals(javaRPb64Result, rollingPeriodb64);

      var transmissionRiskLevel = parts[6].substring(1);
      transmissionRiskLevel = transmissionRiskLevel.substring(0, transmissionRiskLevel.length() - 1);
      var transmissionRiskLevelbytes = ConvertArrayToByteArray(transmissionRiskLevel.split(","));
      var javaTRLb64Result = BatchSignatureUtils.bytesToBase64(transmissionRiskLevelbytes);
      var transmissionRiskLevelb64 = parts[7];

      Assert.assertEquals(javaTRLb64Result, transmissionRiskLevelb64);

      var visitedCountries = parts[8].substring(1);
      visitedCountries = visitedCountries.substring(0, visitedCountries.length() - 1);
      var visitedCountriesbytes = visitedCountries.getBytes(StandardCharsets.US_ASCII);
      var javaVCb64Result = BatchSignatureUtils.bytesToBase64(visitedCountriesbytes);
      var visitedCountriesb64 = parts[9];


      Assert.assertEquals(javaVCb64Result, visitedCountriesb64);

      var origin = parts[10].substring(1);
      origin = origin.substring(0, origin.length() - 1);
      var originbytes = origin.getBytes(StandardCharsets.US_ASCII);
      var javaORb64Result = BatchSignatureUtils.bytesToBase64(originbytes);
      var originb64 = parts[11];

      Assert.assertEquals(javaORb64Result, originb64);

      var reportType = parts[12].substring(1);
      reportType = reportType.substring(0, reportType.length() - 1);
      var reportTypebytes = ConvertArrayToByteArray(reportType.split(","));
      var javaRTb64Result = BatchSignatureUtils.bytesToBase64(reportTypebytes);
      var reportTypeb64 = parts[13];

      Assert.assertEquals(javaRTb64Result, reportTypeb64);

      var dsosType = parts[14].substring(1);
      dsosType = dsosType.substring(0, dsosType.length() - 1);
      var dsosbytes = ConvertArrayToByteArray(dsosType.split(","));
      var dsosb64Result = BatchSignatureUtils.bytesToBase64(dsosbytes);
      var dsosTypeb64 = parts[15];

      Assert.assertEquals(dsosb64Result, dsosTypeb64);


      DiagnosisKey.Builder diagnosisKey = DiagnosisKey.newBuilder();

      diagnosisKey.setKeyData(ByteString.copyFrom(diagnosiskeybytes));
      diagnosisKey.setRollingStartIntervalNumber(ByteBuffer.wrap(rollingStartIntervalNumberbytes).getInt());
      diagnosisKey.setRollingPeriod(ByteBuffer.wrap(rollingPeriodbytes).getInt());
      diagnosisKey.setTransmissionRiskLevel(ByteBuffer.wrap(transmissionRiskLevelbytes).getInt());
      diagnosisKey.addAllVisitedCountries(List.of(visitedCountries.split(",")));
      diagnosisKey.setReportType(eu.interop.federationgateway.model.EfgsProto.ReportType.values()[ByteBuffer.wrap(reportTypebytes).getInt()]);
      diagnosisKey.setDaysSinceOnsetOfSymptoms(ByteBuffer.wrap(dsosbytes).getInt());
      diagnosisKey.setDaysSinceOnsetOfSymptoms(ByteBuffer.wrap(dsosbytes).getInt());
      diagnosisKey.setOrigin(new String(originbytes));

      var overallb64 = BatchSignatureUtils.bytesToBase64(BatchSignatureUtils.generateBytesToVerify(diagnosisKey.build()));
      var overallCSharp64 = parts[16];

      Assert.assertEquals(overallb64, overallCSharp64);

    }
  }

}
