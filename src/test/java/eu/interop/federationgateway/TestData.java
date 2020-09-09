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

package eu.interop.federationgateway;

import com.google.protobuf.ByteString;
import eu.interop.federationgateway.entity.CallbackSubscriptionEntity;
import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyPayload;
import eu.interop.federationgateway.entity.FormatInformation;
import eu.interop.federationgateway.entity.UploaderInformation;
import eu.interop.federationgateway.model.EfgsProto;
import eu.interop.federationgateway.repository.CertificateRepository;
import eu.interop.federationgateway.service.CertificateService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.test.util.ReflectionTestUtils;

public class TestData {

  public static final String FIRST_BATCHTAG = "firstBatchTag";
  public static final String SECOND_BATCHTAG = "secondBatchTag";
  public static final String TEST_BATCH_TAG_2015616 = "2015616-426";
  public static final String FIRST_ORIGIN = "o1";
  public static final String SECOND_ORIGIN = "o2";
  public static final String VISITED_COUNTRIES = "DE,FR,PL";
  public static final String VISITED_COUNTRIES_PLUS_ONE = "DE,FR,KR,PL";
  public static final String VISITED_COUNTRIES_PLUS_ONE_ONLY = "KR";
  public static final String COUNTRY_A = "CA";
  public static final String COUNTRY_B = "CB";
  public static final String COUNTRY_C = "CC";
  public static final String COUNTRY_D = "CD";
  public static final List<String> VISITED_COUNTRIES_LIST = List.of(COUNTRY_A, COUNTRY_B, COUNTRY_C, COUNTRY_D);
  public static final int ROLLING_PERIOD = 1;
  public static final int ROLLING_START_INTERVAL_NUMBER = 2;
  public static final int TRANSMISSION_RISK_LEVEL = 3;
  public static final String PAYLOAD_HASH = "7ad4a1a8e075fe506d944e81f17e4e6c9fb06c9c0743c4c65f18a11685a6a201";
  public static final byte[] BYTES = new byte[]{14, 15, 11, 14, 12, 15, 15, 16};
  public static final String DN_STRING_DE = "C=DE";
  public static final String AUTH_CERT_COUNTRY = "DE";
  public static final String AUTH_VALID_CERT_HASH = "69c697c045b4cdaa441a28af0ec1cc4128153b9ddc796b66bfa04b02ea3e103e";
  public static final String AUTH_CERT_HASH = "af8f74084e27cca43ed9eb0436e906c36c03b14304aa24fe5f9978395d639e3b";
  public static final String SIGNING_CERT_HASH = "9433f80547d70af526a74f8fef35de9bc05c7a29518dd61fa0fd0e955e669e91";
  public static final String CALLBACK_ID_FIRST = "firstCallback";
  public static final String CALLBACK_ID_SECOND = "secondCallback";
  public static final String CALLBACK_URL_EFGS = "https://example.org";
  public static final String CALLBACK_URL_EXAMPLE = "https://example.net";
  public static final int DAYS_SINCE_ONSET_OF_SYMPTOMS = 42;
  public static final String DIGEST_ALGORITHM = "SHA1withRSA";
  private static final String TEST_BATCH_TAG_DE = "uploaderBatchTag_DE";
  private static final String TEST_BATCH_TAG_NL = "uploaderBatchTag_NL";
  private static final String COMMON_NAME_SIGNING_CERT = "demo";
  public static KeyPair keyPair;
  public static X509Certificate expiredCertificate;
  public static X509Certificate validCertificate;
  public static X509Certificate trustAnchor;
  public static String validCertificateHash;
  public static X509Certificate notValidYetCertificate;
  public static X509Certificate manipulatedCertificate;

  private static String insertSigningCertificate(CertificateRepository certificateRepository,
                                                 X509Certificate certificate) throws NoSuchAlgorithmException,
    CertificateEncodingException, IOException, InvalidKeyException, SignatureException {
    byte[] certHashBytes = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
    String certHash = new BigInteger(1, certHashBytes).toString(16);

    String certDn = certificate.getSubjectDN().toString();
    int countryIndex = certDn.indexOf(("C="));
    String certCountry = certDn.substring(countryIndex + 2, countryIndex + 4);

    StringWriter stringWriter = new StringWriter();
    JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
    jcaPEMWriter.writeObject(certificate);
    jcaPEMWriter.flush();
    stringWriter.flush();
    String rawData = stringWriter.toString().replace("\r", "");
    jcaPEMWriter.close();
    stringWriter.close();

    Signature signer = Signature.getInstance("SHA256withRSA");
    signer.initSign(keyPair.getPrivate());
    signer.update(rawData.getBytes());
    byte[] signedData = signer.sign();
    String signature = Base64.getEncoder().encodeToString(signedData);

    CertificateEntity certificateEntity = new CertificateEntity(
      null,
      ZonedDateTime.now(ZoneOffset.UTC),
      certHash,
      certCountry,
      CertificateEntity.CertificateType.SIGNING,
      false,
      null,
      signature,
      rawData
    );

    Optional<CertificateEntity> certInDb = certificateRepository.getFirstByThumbprintAndCountryAndType(
      certHash, certCountry, CertificateEntity.CertificateType.SIGNING);

    certInDb.ifPresent(certificateRepository::delete);

    certificateRepository.save(certificateEntity);
    return certHash;
  }

  public static void insertCertificatesForAuthentication(CertificateRepository certificateRepository)
    throws NoSuchAlgorithmException, CertificateException, IOException, OperatorCreationException,
    InvalidKeyException, SignatureException, KeyStoreException {
    createCertificates();

    validCertificateHash = insertSigningCertificate(certificateRepository, validCertificate);
    insertSigningCertificate(certificateRepository, expiredCertificate);
    insertSigningCertificate(certificateRepository, notValidYetCertificate);
    insertSigningCertificate(certificateRepository, manipulatedCertificate);

    manipulateCertificate();

    CertificateEntity authCertificateEntity = new CertificateEntity(
      null,
      ZonedDateTime.now(ZoneOffset.UTC),
      AUTH_CERT_HASH,
      AUTH_CERT_COUNTRY,
      CertificateEntity.CertificateType.AUTHENTICATION,
      false,
      null,
      "Gmf1DsCtv5gXyaaHdswRjTKze6QNJ7UIbZpxAgsl5ODH1W8ZYoNJAbgINfWCbWEAIZBlSaHHWikSKTP3gGE4Ne6toxr19t3DP"
        + "+6QTWpzY5uzaamOmD12YfgaIfI4TuLYcV+YEcp6Vq29871igXiGNe49le/tK4C6CbPMKuOuUKaGwKdF5tC++b+frjsI11IGpNwwGBG"
        + "+GuUcuBWwhtdo3+T+3gVAjneSAWkCc15lhtvlh66LOTQ+luGpYuIiJRh0US6B8dWG5FdOSnuWQ/PoxoqXpaQ4wlqEeJD9K"
        + "+UZ2Z2ehLXwrq/nvn3ZbQiyomB9rGiEG/USpQl4HHOvRM253g==",
      "-----BEGIN CERTIFICATE-----\n"
        + "MIIGpjCCBI6gAwIBAgIUUsiTxuVFRw7WoKKnDVsRmQd5uFowDQYJKoZIhvcNAQEL\n"
        + "BQAwgbQxCzAJBgNVBAYTAkRFMRUwEwYDVQQHDAxTYWFyYnLDvGNrZW4xJTAjBgNV\n"
        + "BAoTHFQtU3lzdGVtcyBJbnRlcm5hdGlvbmFsIEdtYkgxFjAUBgNVBAsTDUNXQSBU\n"
        + "ZXN0IFRlYW0xIDAeBgNVBAMTF0VGR1MgU2ltdWxhdG9yIFRlc3QgQ0EyMS0wKwYJ\n"
        + "KoZIhvcNAQkBFh5Db3JvbmFBcHBUZXN0aW5nQG1nLnRlbGVrb20uZGUwHhcNMjAw\n"
        + "OTAyMTYzMDIyWhcNMjMwOTAzMTYzMDIyWjCBnDELMAkGA1UEBhMCQkUxEDAOBgNV\n"
        + "BAgMB0JlbGdpdW0xEDAOBgNVBAcMB0JlbGdpdW0xFzAVBgNVBAoMDkVGR1MtVGVz\n"
        + "dC1UZWFtMRcwFQYDVQQLDA5FRkdTLVRlc3QtVGVhbTE3MDUGA1UEAwwuY3dhLXNp\n"
        + "bXVsYXRvci1iZS53ZXN0ZXVyb3BlLmNsb3VkYXBwLmF6dXJlLmNvbTCCAiIwDQYJ\n"
        + "KoZIhvcNAQEBBQADggIPADCCAgoCggIBALp+Js+W9YMC6pN39OHrT1p32dbhXmzW\n"
        + "W1NdRP//+Klrr8bsNiGNf74UQS9HA4uI+whHYfelx/nwlXFBeaCIQs5EL1sNSmRk\n"
        + "2f3kgMP53RO42MCoXXTsAAjOtVl2h1zOduNiTgqlIAxdc/tMkPewV4izWxBLThmG\n"
        + "jjlTFh81xFymWFRKz4pZmWbpgdrR0Smp0fJG1HOWvqKGX8Z9A9agLfLMapLorqVH\n"
        + "uC/6wXbYj7RYcMkUMr85fjmNzc8OKLs6OQVZ552V7OgY/7D5P2mXE8Twdb+ama+7\n"
        + "q9tHByF6ohbWNb2mywEsCeh+pYSTD4Qh2V7S2Z6o6eaPuq7sbOvPVT9hEc8InHHY\n"
        + "J9PPUfJqPU8FhEHEKnqz1L/MrcD+JlmT3EO7y/btVRRE+A+KWXlVK7cvtw89f5u5\n"
        + "2cZhyPxB8Z74oWr2BodCaNjcjdqT67+x5muKPVRzAXiHUuiWEuHki2Is3IX5FLTm\n"
        + "c9qcovqjdgFOmpLJWKi12MUA8jkLYaRjgUULoSb+9vlRS6bQ8ix6bbTa9CqAsbQ5\n"
        + "Gce01E+3SZt5jB6tEDgFQjGs+ZX4aUVibGc0C3tHwExgM1R/hUmYVZ/X6u8/+/1P\n"
        + "/1M72GusQqzbBWTbmciixME9Xu+L+UrdXoGHjhLbxg1wQ/LBRdPzcPqq0wk1S29F\n"
        + "7idSZ2NGBK/dAgMBAAGjgcUwgcIwCQYDVR0TBAIwADARBglghkgBhvhCAQEEBAMC\n"
        + "BaAwMwYJYIZIAYb4QgENBCYWJE9wZW5TU0wgR2VuZXJhdGVkIENsaWVudCBDZXJ0\n"
        + "aWZpY2F0ZTAdBgNVHQ4EFgQUMvQnnxeK0JmtnY9Q9U9SR+Td40YwHwYDVR0jBBgw\n"
        + "FoAUSm16Zq46oc49eDPY7v1bay5xNygwDgYDVR0PAQH/BAQDAgXgMB0GA1UdJQQW\n"
        + "MBQGCCsGAQUFBwMCBggrBgEFBQcDBDANBgkqhkiG9w0BAQsFAAOCAgEAdsitfnED\n"
        + "cmXWkCzyldmKUUZk6FLv8Mr3ohhwt/8XPWZUuRMQNrdsxDGg7XH6r6dfJpsG6e+h\n"
        + "vkRVAKcJ1i7FaKDm40wgrzrOZk6H8DLzoA+4O0gSGUqqvWzZUW149naYlzYO7ZNm\n"
        + "XBXb8vFi4bUiyoBzaMihW2FpKXj/B+0lGrgOVgOaiyHt4mrebye4H0Nhq5kE8Fd1\n"
        + "Ka5bJjsTq06i3BtMz8fZg6YUyHmKp++Bw57F4mEaHHW0dvmMxX5M4omTrQRrxeyL\n"
        + "4prV/kmfUGvD1/u1wgUDrjpl0P5FEeb0gZXZi8ofzWOz9Rs/qGgN5UsH4hDTst0x\n"
        + "ds9FzCmiYTAOQq7beGLAbTAyNyW34VNGYghcBG0pioSROZLaOVQ6KaYH5NTqHT4+\n"
        + "gvEkbSytKLBkoyvKxX1UrgYFfttUMLnho5yaxctLL4PAkQqEhjDNrNr/9i+e9qvU\n"
        + "qQN6/YXXOaLOSuoJGX2WOTnaaOtexANE9u8ATByWinx3XmxpR9gMRzNbkMR6nm2B\n"
        + "v79GsuubnjF7e0ZUiCbjwAqLipMQbM9JDaxU9zS+lh4OszcM7rs+bFCUaxl067Ng\n"
        + "WdOu75AfKTI83b6G0hOin9N/mDlW9df2Qu4wegXllC0oo1rIKNIWx0ECFHvCFBWF\n"
        + "Os3mHyNHl7fVzpJ9VrnXgWuRnR3pZu+KeB4=\n"
        + "-----END CERTIFICATE-----"
    );

    Optional<CertificateEntity> authCertInDb = certificateRepository.getFirstByThumbprintAndCountryAndType(
      AUTH_CERT_HASH, AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.AUTHENTICATION);

    authCertInDb.ifPresent(certificateRepository::delete);

    certificateRepository.save(authCertificateEntity);

  }
  private static X509Certificate generateCertificate(Date validFrom, Date validTo) throws OperatorCreationException,
    CertIOException, CertificateException {
    X500Name dnName = new X500Name("C=" + TestData.AUTH_CERT_COUNTRY + ", CN=" + COMMON_NAME_SIGNING_CERT);
    BigInteger certSerial = new BigInteger(Long.toString(System.currentTimeMillis()));

    ContentSigner contentSigner = new JcaContentSignerBuilder(DIGEST_ALGORITHM).build(TestData.keyPair.getPrivate());
    JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(dnName, certSerial, validFrom, validTo,
      dnName, TestData.keyPair.getPublic());

    BasicConstraints basicConstraints = new BasicConstraints(false);
    certBuilder.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), true, basicConstraints);

    return new JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner));
  }

  public static void createCertificates() throws NoSuchAlgorithmException, CertificateException, CertIOException,
    OperatorCreationException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    TestData.keyPair = keyGen.generateKeyPair();

    TestData.validCertificate = generateCertificate(
      Date.from(ZonedDateTime.now().minusDays(14).toInstant()),
      Date.from(ZonedDateTime.now().plusYears(1).toInstant())
    );

    TestData.expiredCertificate = generateCertificate(
      Date.from(ZonedDateTime.now().minusDays(14).toInstant()),
      Date.from(ZonedDateTime.now().minusDays(1).toInstant())
    );

    TestData.notValidYetCertificate = generateCertificate(
      Date.from(ZonedDateTime.now().plusDays(1).toInstant()),
      Date.from(ZonedDateTime.now().plusDays(14).toInstant())
    );

    TestData.manipulatedCertificate = generateCertificate(
      Date.from(ZonedDateTime.now().minusDays(14).toInstant()),
      Date.from(ZonedDateTime.now().plusDays(14).toInstant())
    );

    TestData.trustAnchor = generateCertificate(
      Date.from(ZonedDateTime.now().minusDays(14).toInstant()),
      Date.from(ZonedDateTime.now().plusYears(1).toInstant())
    );
  }

  private static void manipulateCertificate() throws CertificateException {
    byte[] certBytes = TestData.manipulatedCertificate.getEncoded();
    byte[] search = COMMON_NAME_SIGNING_CERT.getBytes();

    // Search for our Common Name in ByteStream and manipulate it a little bit to break the signature.
    for (int i = 0; i < certBytes.length - search.length; i++) {
      if (certBytes[i] == search[0]) {
        boolean match = true;
        for (int j = 0; j < search.length; j++) {
          if (certBytes[i + j] != search[j]) {
            match = false;
            break;
          }
        }

        if (match) {
          certBytes[i] = (byte) (search[0] + 1);
        }
      }
    }

    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    TestData.manipulatedCertificate =
      (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certBytes));
  }

  public static EfgsProto.DiagnosisKey getDiagnosisKeyProto() {
    return EfgsProto.DiagnosisKey.newBuilder()
      .setRollingPeriod(TestData.ROLLING_PERIOD)
      .setRollingStartIntervalNumber(TestData.ROLLING_START_INTERVAL_NUMBER)
      .setTransmissionRiskLevel(TestData.TRANSMISSION_RISK_LEVEL)
      .setOrigin(TestData.AUTH_CERT_COUNTRY)
      .setReportType(EfgsProto.ReportType.CONFIRMED_CLINICAL_DIAGNOSIS)
      .setKeyData(ByteString.copyFrom(TestData.BYTES))
      .setDaysSinceOnsetOfSymptoms(TestData.DAYS_SINCE_ONSET_OF_SYMPTOMS)
      .addAllVisitedCountries(Arrays.asList(VISITED_COUNTRIES.split(",")))
      .build();
  }

  public static DiagnosisKeyEntity getDiagnosisKeyTestEntityforCreation() {
    return new DiagnosisKeyEntity(
      null,
      null,
      null,
      PAYLOAD_HASH,
      new DiagnosisKeyPayload(
        BYTES,
        ROLLING_START_INTERVAL_NUMBER,
        ROLLING_PERIOD,
        TRANSMISSION_RISK_LEVEL,
        VISITED_COUNTRIES,
        AUTH_CERT_COUNTRY,
        DiagnosisKeyPayload.ReportType.CONFIRMED_CLINICAL_DIAGNOSIS,
        DAYS_SINCE_ONSET_OF_SYMPTOMS
      ),
      new FormatInformation(1, 0),
      new UploaderInformation(FIRST_BATCHTAG, "b", "c", "d", "e")
    );
  }

  public static List<DiagnosisKeyEntity> createTestDiagKeysWithoutBatchTag() throws NoSuchAlgorithmException {

    List<DiagnosisKeyEntity> testKeys = new ArrayList<>();

    // key 1
    DiagnosisKeyEntity diagnosisKeyEntity_1 = new DiagnosisKeyEntity();
    diagnosisKeyEntity_1.setBatchTag(null);
    diagnosisKeyEntity_1.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
    DiagnosisKeyPayload diagnosisKeyPayload = new DiagnosisKeyPayload();
    diagnosisKeyPayload.setKeyData("123".getBytes());
    diagnosisKeyPayload.setOrigin("DE");
    diagnosisKeyPayload.setRollingPeriod(2);
    diagnosisKeyPayload.setRollingStartIntervalNumber(123);
    diagnosisKeyPayload.setTransmissionRiskLevel(17);
    diagnosisKeyPayload.setReportType(DiagnosisKeyPayload.ReportType.CONFIRMED_CLINICAL_DIAGNOSIS);
    diagnosisKeyPayload.setVisitedCountries("NL, UK, FR, AT");
    diagnosisKeyEntity_1.setPayload(diagnosisKeyPayload);
    diagnosisKeyEntity_1.setPayloadHash(new String(MessageDigest.getInstance("SHA-256").digest(diagnosisKeyPayload.toString().getBytes())));
    UploaderInformation uploaderInformation = new UploaderInformation();
    uploaderInformation.setBatchSignature("batchSignature");
    uploaderInformation.setBatchTag(TEST_BATCH_TAG_DE);
    uploaderInformation.setCountry("DE");
    uploaderInformation.setThumbprint("thumbprint");
    diagnosisKeyEntity_1.setUploader(uploaderInformation);
    FormatInformation formatInformation = new FormatInformation();
    formatInformation.setMajorVersion(1);
    formatInformation.setMinorVersion(0);
    diagnosisKeyEntity_1.setFormat(formatInformation);

    testKeys.add(diagnosisKeyEntity_1);

    // key 2
    DiagnosisKeyEntity diagnosisKeyEntity_2 = new DiagnosisKeyEntity();
    diagnosisKeyEntity_2.setBatchTag(null);
    diagnosisKeyEntity_2.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
    diagnosisKeyPayload = new DiagnosisKeyPayload();
    diagnosisKeyPayload.setKeyData("234".getBytes());
    diagnosisKeyPayload.setOrigin("DE");
    diagnosisKeyPayload.setRollingPeriod(2);
    diagnosisKeyPayload.setRollingStartIntervalNumber(123);
    diagnosisKeyPayload.setTransmissionRiskLevel(17);
    diagnosisKeyPayload.setReportType(DiagnosisKeyPayload.ReportType.CONFIRMED_CLINICAL_DIAGNOSIS);
    diagnosisKeyPayload.setVisitedCountries("UK, FR, AT");
    diagnosisKeyEntity_2.setPayload(diagnosisKeyPayload);
    diagnosisKeyEntity_2.setPayloadHash(new String(MessageDigest.getInstance("SHA-256").digest(diagnosisKeyPayload.toString().getBytes())));
    uploaderInformation = new UploaderInformation();
    uploaderInformation.setBatchSignature("batchSignature");
    uploaderInformation.setBatchTag(TEST_BATCH_TAG_DE);
    uploaderInformation.setCountry("DE");
    uploaderInformation.setThumbprint("thumbprint");
    diagnosisKeyEntity_2.setUploader(uploaderInformation);
    formatInformation = new FormatInformation();
    formatInformation.setMajorVersion(1);
    formatInformation.setMinorVersion(0);
    diagnosisKeyEntity_2.setFormat(formatInformation);

    testKeys.add(diagnosisKeyEntity_2);

    // key 3
    DiagnosisKeyEntity diagnosisKeyEntity_3 = new DiagnosisKeyEntity();
    diagnosisKeyEntity_3.setBatchTag(null);
    diagnosisKeyEntity_3.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
    diagnosisKeyPayload = new DiagnosisKeyPayload();
    diagnosisKeyPayload.setKeyData("345".getBytes());
    diagnosisKeyPayload.setOrigin("DE");
    diagnosisKeyPayload.setRollingPeriod(2);
    diagnosisKeyPayload.setRollingStartIntervalNumber(123);
    diagnosisKeyPayload.setTransmissionRiskLevel(17);
    diagnosisKeyPayload.setReportType(DiagnosisKeyPayload.ReportType.CONFIRMED_CLINICAL_DIAGNOSIS);
    diagnosisKeyPayload.setVisitedCountries("FR, AT");
    diagnosisKeyEntity_3.setPayload(diagnosisKeyPayload);
    diagnosisKeyEntity_3.setPayloadHash(new String(MessageDigest.getInstance("SHA-256").digest(diagnosisKeyPayload.toString().getBytes())));
    uploaderInformation = new UploaderInformation();
    uploaderInformation.setBatchSignature("batchSignature");
    uploaderInformation.setBatchTag(TEST_BATCH_TAG_NL);
    uploaderInformation.setCountry("NL");
    uploaderInformation.setThumbprint("thumbprint");
    diagnosisKeyEntity_3.setUploader(uploaderInformation);
    formatInformation = new FormatInformation();
    formatInformation.setMajorVersion(1);
    formatInformation.setMinorVersion(0);
    diagnosisKeyEntity_3.setFormat(formatInformation);

    testKeys.add(diagnosisKeyEntity_3);

    return testKeys;
  }

  public static List<DiagnosisKeyEntity> createTestDiagKeyWithBatchTag() throws NoSuchAlgorithmException {

    List<DiagnosisKeyEntity> testKeys = new ArrayList<>();

    DiagnosisKeyEntity diagnosisKeyEntity_1 = new DiagnosisKeyEntity();
    diagnosisKeyEntity_1.setBatchTag(TEST_BATCH_TAG_2015616);
    diagnosisKeyEntity_1.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
    DiagnosisKeyPayload diagnosisKeyPayload = new DiagnosisKeyPayload();
    diagnosisKeyPayload.setKeyData("123".getBytes());
    diagnosisKeyPayload.setOrigin("DE");
    diagnosisKeyPayload.setRollingPeriod(2);
    diagnosisKeyPayload.setRollingStartIntervalNumber(123);
    diagnosisKeyPayload.setTransmissionRiskLevel(17);
    diagnosisKeyPayload.setReportType(DiagnosisKeyPayload.ReportType.CONFIRMED_CLINICAL_DIAGNOSIS);
    diagnosisKeyPayload.setVisitedCountries("AT");
    diagnosisKeyEntity_1.setPayload(diagnosisKeyPayload);
    diagnosisKeyEntity_1.setPayloadHash(new String(MessageDigest.getInstance("SHA-256").digest(diagnosisKeyPayload.toString().getBytes())));
    UploaderInformation uploaderInformation = new UploaderInformation();
    uploaderInformation.setBatchSignature("batchSignature");
    uploaderInformation.setBatchTag("uploaderBatchTag_AT");
    uploaderInformation.setCountry("AT");
    uploaderInformation.setThumbprint("thumbprint");
    diagnosisKeyEntity_1.setUploader(uploaderInformation);
    FormatInformation formatInformation = new FormatInformation();
    formatInformation.setMajorVersion(1);
    formatInformation.setMinorVersion(0);
    diagnosisKeyEntity_1.setFormat(formatInformation);

    testKeys.add(diagnosisKeyEntity_1);

    return testKeys;
  }

  public static List<DiagnosisKeyEntity> createTestDiagKeysList(int count, String batchTag, String origin) throws NoSuchAlgorithmException {
    return createTestDiagKeysList(count, batchTag, origin, 1, 0);
  }

  public static List<DiagnosisKeyEntity> createTestDiagKeysList(int count, String batchTag, String origin,
                                                                int majorVersion, int minorVersion) throws NoSuchAlgorithmException {

    List<DiagnosisKeyEntity> testKeys = new ArrayList<>();
    Random random = new Random();

    for (int i = 0; i < count; i++) {
      DiagnosisKeyEntity key = new DiagnosisKeyEntity();
      key.setBatchTag(null);
      key.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
      DiagnosisKeyPayload diagnosisKeyPayload = new DiagnosisKeyPayload();
      diagnosisKeyPayload.setKeyData((batchTag + i).getBytes());
      diagnosisKeyPayload.setOrigin(origin);
      diagnosisKeyPayload.setRollingPeriod(2);
      diagnosisKeyPayload.setRollingStartIntervalNumber(random.nextInt(500 + 1 - 100) + 100 + i);
      diagnosisKeyPayload.setTransmissionRiskLevel(17);
      diagnosisKeyPayload.setReportType(DiagnosisKeyPayload.ReportType.CONFIRMED_CLINICAL_DIAGNOSIS);
      diagnosisKeyPayload.setVisitedCountries("AT");
      key.setPayload(diagnosisKeyPayload);
      key.setPayloadHash(new String(MessageDigest.getInstance("SHA-256").digest(diagnosisKeyPayload.toString().getBytes())));
      UploaderInformation uploaderInformation = new UploaderInformation();
      uploaderInformation.setBatchSignature("batchSignature");
      uploaderInformation.setBatchTag(batchTag);
      uploaderInformation.setCountry("DE");
      uploaderInformation.setThumbprint("thumbprint");
      key.setUploader(uploaderInformation);
      FormatInformation formatInformation = new FormatInformation();
      formatInformation.setMajorVersion(majorVersion);
      formatInformation.setMinorVersion(minorVersion);
      key.setFormat(formatInformation);
      testKeys.add(key);
    }

    return testKeys;
  }

  public static CallbackSubscriptionEntity createTestCallback(String id, String url, String country) {
    CallbackSubscriptionEntity callbackSubscriptionEntity = new CallbackSubscriptionEntity();
    callbackSubscriptionEntity.setCallbackId(id);
    callbackSubscriptionEntity.setUrl(url);
    callbackSubscriptionEntity.setCountry(country);
    callbackSubscriptionEntity.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
    return callbackSubscriptionEntity;
  }
}
