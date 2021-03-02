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

package eu.interop.federationgateway.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CertificateUtils {

  /**
   * Calculates the SHA-256 thumbprint of X509Certificate.
   *
   * @param x509Certificate the certificate the thumbprint should be calculated
   *                        for.
   * @return 32-byte SHA-256 hash as hex encoded string
   */
  public static String getCertThumbprint(X509Certificate x509Certificate) {
    try {
      return calculateHash(x509Certificate.getEncoded());
    } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
      log.error("Could not calculate thumbprint of certificate.");
      return null;
    }
  }

  /**
   * Calculates the SHA-256 thumbprint of X509CertificateHolder.
   *
   * @param x509CertificateHolder the certificate the thumbprint should be
   *                              calculated for.
   * @return 32-byte SHA-256 hash as hex encoded string
   */
  public static String getCertThumbprint(X509CertificateHolder x509CertificateHolder) {
    try {
      return calculateHash(x509CertificateHolder.getEncoded());
    } catch (IOException | NoSuchAlgorithmException e) {
      log.error("Could not calculate thumbprint of certificate.");
      return null;
    }
  }

  /**
   * Assumes we have a certificate in DER representation, somehow encoded to be
   * transmitted in a header. This means either just the base64encoding of the DER
   * representation, or any valid way of transmitting the PEM file, like escaping
   * newlines or url encode newlines.
   * 
   * <p>We normalize the data and try to extract the DER bytes of the certificate,
   * which we feed to another function, which then uses a {@link CertificateFactory} to get
   * the encoded object.
   *
   *
   * @param rawData the certificate as a PEM-String or quasi PEM-String (PEM
   *                without newlines, no suffix/prefix).
   * @return X509Certificate of rawData, or null if it could not be parsed
   */
  public static X509Certificate getCertificateFromRawString(String rawData) {
    String normalizedData = rawData;
    // Check if there is a % in the header (we assume this indicates urlencoding)
    if (rawData.contains("%")) {
      try {
        normalizedData = URLDecoder.decode(normalizedData, StandardCharsets.UTF_8);
      } catch (IllegalArgumentException ex) {
        log.info("Data contains invalid url encoded characters, skipping");
      }
    }

    normalizedData = normalizedData
        // remove PEM prefix
        .replace("-----BEGIN CERTIFICATE-----", "")
        // remove escaped whitespaces
        .replace("\\\\n", "").replace("\\n", "").replace("\\\\r", "").replace("\\r", "")
        // remove PEM suffix
        .replace("-----END CERTIFICATE-----", "")
        // remove all whitespaces
        .replace("\n", "")
        .replace("\r", "")
        .replace(" ", "").trim();
    // now we should have a base64 encoded string of the DER-representation of the
    // certificate
    try {
      byte[] derBytes = Base64.getDecoder().decode(normalizedData);
      return getX509CertificateFromDer(derBytes);
    } catch (IllegalArgumentException argumentException) {
      log.error("Data is not valid base64");
      return null;
    }
  }

  /**
   * Gets the X509Certificate from a DER representation thereof. This functions
   * expects the bytes to be in the correct format.
   *
   * @param der the certificaten as a DER-bytes.
   * @return X509Certificate of the DER bytes, or null if it could not be parsed
   */
  public static X509Certificate getX509CertificateFromDer(byte[] der) {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(der);
    try {
      Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(byteArrayInputStream);
      return certificate instanceof X509Certificate ? (X509Certificate) certificate : null;
    } catch (CertificateException ignored) {
      return null;
    }
  }

  private static String calculateHash(byte[] data) throws NoSuchAlgorithmException {
    byte[] certHashBytes = MessageDigest.getInstance("SHA-256").digest(data);
    String hexString = new BigInteger(1, certHashBytes).toString(16);

    if (hexString.length() == 63) {
      hexString = "0" + hexString;
    }

    return hexString;
  }
}
