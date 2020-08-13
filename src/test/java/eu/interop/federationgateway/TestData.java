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
import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyPayload;
import eu.interop.federationgateway.entity.FormatInformation;
import eu.interop.federationgateway.entity.UploaderInformation;
import eu.interop.federationgateway.model.EfgsProto;
import eu.interop.federationgateway.repository.CertificateRepository;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

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
  public static final String AUTH_CERT_HASH = "69c697c045b4cdaa441a28af0ec1cc4128153b9ddc796b66bfa04b02ea3e103e";
  public static final int DAYS_SINCE_ONSET_OF_SYMPTOMS = 42;
  public static final String DIGEST_ALGORITHM = "SHA1withRSA";
  public static final String CRYPTO_PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
  private static final String TEST_BATCH_TAG_DE = "uploaderBatchTag_DE";
  private static final String TEST_BATCH_TAG_NL = "uploaderBatchTag_NL";
  private static final String COMMON_NAME_SIGNING_CERT = "demo";
  public static KeyPair keyPair;
  public static X509Certificate expiredCertificate;
  public static X509Certificate validCertificate;
  public static String validCertificateHash;
  public static X509Certificate notValidYetCertificate;
  public static X509Certificate manipulatedCertificate;

  private static String insertSigningCertificate(CertificateRepository certificateRepository, X509Certificate certificate) throws NoSuchAlgorithmException, CertificateEncodingException {
    byte[] certHashBytes = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
    String certHash = new BigInteger(1, certHashBytes).toString(16);

    String certDn = certificate.getSubjectDN().toString();
    int countryIndex = certDn.indexOf(("C="));
    String certCountry = certDn.substring(countryIndex + 2, countryIndex + 4);

    CertificateEntity certificateEntity = new CertificateEntity(
      null,
      ZonedDateTime.now(ZoneOffset.UTC),
      certHash,
      certCountry,
      CertificateEntity.CertificateType.SIGNING,
      false
    );

    Optional<CertificateEntity> certInDb = certificateRepository.getFirstByThumbprintAndCountryAndType(
      certHash, certCountry, CertificateEntity.CertificateType.SIGNING);

    certInDb.ifPresent(certificateRepository::delete);

    certificateRepository.save(certificateEntity);
    return certHash;
  }

  public static void insertCertificatesForAuthentication(CertificateRepository certificateRepository) throws NoSuchAlgorithmException, CertificateException, CertIOException, OperatorCreationException {
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
      false
    );

    Optional<CertificateEntity> authCertInDb = certificateRepository.getFirstByThumbprintAndCountryAndType(
      AUTH_CERT_HASH, AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.AUTHENTICATION);

    authCertInDb.ifPresent(certificateRepository::delete);

    certificateRepository.save(authCertificateEntity);
  }

  private static X509Certificate generateCertificate(Date validFrom, Date validTo) throws OperatorCreationException, CertIOException, CertificateException {
    X500Name dnName = new X500Name("C=" + TestData.AUTH_CERT_COUNTRY + ", CN=" + COMMON_NAME_SIGNING_CERT);
    BigInteger certSerial = new BigInteger(Long.toString(System.currentTimeMillis()));

    ContentSigner contentSigner = new JcaContentSignerBuilder(DIGEST_ALGORITHM).setProvider(CRYPTO_PROVIDER).build(TestData.keyPair.getPrivate());
    JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(dnName, certSerial, validFrom, validTo, dnName, TestData.keyPair.getPublic());

    BasicConstraints basicConstraints = new BasicConstraints(false);
    certBuilder.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), true, basicConstraints);

    return new JcaX509CertificateConverter().setProvider(Security.getProvider(CRYPTO_PROVIDER)).getCertificate(certBuilder.build(contentSigner));
  }

  public static void createCertificates() throws NoSuchAlgorithmException, CertificateException, CertIOException, OperatorCreationException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    TestData.keyPair = keyGen.generateKeyPair();

    TestData.validCertificate = generateCertificate(
      Date.from(ZonedDateTime.now().minusDays(14).toInstant()),
      Date.from(ZonedDateTime.now().plusDays(14).toInstant())
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
    TestData.manipulatedCertificate = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certBytes));
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
      new UploaderInformation(FIRST_BATCHTAG, "b", "c", "d")
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
      formatInformation.setMajorVersion(1);
      formatInformation.setMinorVersion(0);
      key.setFormat(formatInformation);
      testKeys.add(key);
    }

    return testKeys;
  }
}
