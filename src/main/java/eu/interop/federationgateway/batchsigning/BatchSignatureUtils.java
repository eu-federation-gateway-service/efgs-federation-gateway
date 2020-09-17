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

import com.google.protobuf.ProtocolStringList;
import eu.interop.federationgateway.model.EfgsProto.DiagnosisKey;
import eu.interop.federationgateway.model.EfgsProto.DiagnosisKeyBatch;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class provides help methods used by {@link BatchSignatureVerifier} to verify a batch signature.
 */
@Slf4j
public class BatchSignatureUtils {

  private BatchSignatureUtils() {
    throw new IllegalStateException("Class has no public constructor!");
  }

  /**
   * Extracts the information (e.g., keyData, rollingPeriod, origin, etc.) from a {@link DiagnosisKeyBatch} object,
   * and generates with it a byte stream used to verify the batch signature. The created byte stream has an order
   * defined in the Federation Gateway specification.
   *
   * @param batch the diagnosis key batch, from which the information to generate the bytes to verify are obtained.
   * @return the bytes that will be used to verify the batch signature.
   */
  static byte[] generateBytesToVerify(final DiagnosisKeyBatch batch) {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    sortBatchByKeyData(batch)
      .forEach(diagnosisKey -> byteArrayOutputStream.writeBytes(generateBytesToVerify(diagnosisKey)));

    return byteArrayOutputStream.toByteArray();
  }

  /**
   * Extracts the information (e.g., keyData, rollingPeriod, origin, etc.) from a {@link DiagnosisKey} object,
   * and generates with it a byte stream used to verify the batch signature for one entity.
   * The created byte stream has an order defined in the Federation Gateway specification.
   *
   * @param diagnosisKey the diagnosis key, from which the information to generate the bytes to verify are obtained.
   * @return the bytes that will be used to verify the key signature.
   */
  public static byte[] generateBytesToVerify(final DiagnosisKey diagnosisKey) {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    writeStringInByteArray(diagnosisKey.getKeyData().toStringUtf8(), byteArrayOutputStream);
    writeIntInByteArray(diagnosisKey.getRollingStartIntervalNumber(), byteArrayOutputStream);
    writeIntInByteArray(diagnosisKey.getRollingPeriod(), byteArrayOutputStream);
    writeIntInByteArray(diagnosisKey.getTransmissionRiskLevel(), byteArrayOutputStream);
    writeVisitedCountriesInByteArray(diagnosisKey.getVisitedCountriesList(), byteArrayOutputStream);
    writeStringInByteArray(diagnosisKey.getOrigin(), byteArrayOutputStream);
    writeIntInByteArray(diagnosisKey.getReportTypeValue(), byteArrayOutputStream);
    writeIntInByteArray(diagnosisKey.getDaysSinceOnsetOfSymptoms(), byteArrayOutputStream);

    return byteArrayOutputStream.toByteArray();
  }

  /**
   * Converts a Base64 string into a byte array.
   *
   * @param batchSignatureBase64 the base64 string of the batch signature.
   * @return the batch signature decoded as byte array. Returns an empty array if conversion failed.
   */
  static byte[] b64ToBytes(final String batchSignatureBase64) {
    try {
      return Base64.getDecoder().decode(batchSignatureBase64.getBytes());
    } catch (IllegalArgumentException e) {
      log.error("Failed to convert base64 to byte array");
      return new byte[0];
    }
  }

  private static List<DiagnosisKey> sortBatchByKeyData(DiagnosisKeyBatch batch) {
    return batch.getKeysList()
      .stream()
      .sorted(Comparator.comparing(diagnosisKey -> diagnosisKey.getKeyData().toStringUtf8()))
      .collect(Collectors.toList());
  }

  private static void writeStringInByteArray(final String batchString, final ByteArrayOutputStream byteArray) {
    byteArray.writeBytes(batchString.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeIntInByteArray(final int batchInt, final ByteArrayOutputStream byteArray) {
    byteArray.writeBytes(ByteBuffer.allocate(4).putInt(batchInt).array());
  }

  private static void writeVisitedCountriesInByteArray(final ProtocolStringList countries,
                                                       final ByteArrayOutputStream byteArray) {
    final List<String> countriesList = new ArrayList<>();
    countries.iterator().forEachRemaining(countriesList::add);
    countriesList.sort(String::compareTo);
    for (final String country : countriesList) {
      writeStringInByteArray(country, byteArray);
    }
  }

}
