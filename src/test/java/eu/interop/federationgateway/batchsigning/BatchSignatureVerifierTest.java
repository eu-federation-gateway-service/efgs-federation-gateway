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

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.model.EfgsProto.DiagnosisKeyBatch;
import eu.interop.federationgateway.repository.CertificateRepository;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.List;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class BatchSignatureVerifierTest {

  @Autowired
  BatchSignatureVerifier batchSignatureVerifier;

  @Autowired
  CertificateRepository certificateRepository;

  SignatureGenerator signatureGenerator;

  @Before
  public void setup() throws NoSuchAlgorithmException, CertificateException, CertIOException, OperatorCreationException {
    signatureGenerator = new SignatureGenerator(certificateRepository);
  }

  @Test
  public void testVerifyReturnsTrue()
    throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("XYZ", "ABC"));
    Assert.assertTrue(batchSignatureVerifier.verify(batch, createSignature(batch, TestData.validCertificate)));
  }

  @Test
  public void testVerifyReturnsFalseWhenCertIsExpired()
    throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("XYZ", "ABC"));
    Assert.assertFalse(batchSignatureVerifier.verify(batch, createSignature(batch, TestData.expiredCertificate)));
  }

  @Test
  public void testVerifyReturnsFalseWhenCertIsNotValidYet()
    throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("XYZ", "ABC"));
    Assert.assertFalse(batchSignatureVerifier.verify(batch, createSignature(batch, TestData.notValidYetCertificate)));
  }

  @Test
  public void testVerifyReturnsFalseWhenCertIsManipulated()
    throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("XYZ", "ABC"));
    Assert.assertFalse(batchSignatureVerifier.verify(batch, createSignature(batch, TestData.manipulatedCertificate)));
  }

  @Test
  public void testVerifyReturnsTrueWhenKeyDataOrderIsNotEqualForSignerAndVerifier()
    throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final DiagnosisKeyBatch batchVerifier = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("XYZ", "ABC", "DFG"));
    final DiagnosisKeyBatch batchSigner = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("DFG", "XYZ", "ABC"));
    Assert.assertTrue(batchSignatureVerifier.verify(batchVerifier, createSignature(batchSigner, TestData.validCertificate)));
  }

  @Test
  public void testVerifyReturnsFalseWhenCorruptedBatchBytesAreSigned()
    throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("XYZ", "ABC"));
    Assert.assertFalse(batchSignatureVerifier.verify(batch, createSignatureWithCorruptedBatchBytes(batch)));
  }

  @Test
  public void testVerifyReturnsFalseWhenBase64SignatureIsCorrupted()
    throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("XYZ", "ABC"));
    Assert.assertFalse(batchSignatureVerifier.verify(batch, createSignatureWithCorruptedB64String(batch)));
  }

  @Test
  public void testVerifyReturnsFalseWhenIncorrectBatchIsSigned()
    throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("123", "456"));
    final DiagnosisKeyBatch incorrectBatch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("123"));
    Assert.assertFalse(batchSignatureVerifier.verify(batch, createSignature(incorrectBatch, TestData.validCertificate)));
  }

  @Test
  public void testVerifyReturnsFalseWhenBatchWithIncorrectByteOrderIsSigned()
    throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("1AB8C3"));
    Assert.assertFalse(batchSignatureVerifier.verify(batch, createInvalidSignatureForBatchWithIncorrectByteOder(batch)));
  }

  @Test
  public void testVerifyReturnsFalseWhenCertSignatureIsInvalid() throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("XYZ", "ABC"));

    certificateRepository.deleteAll();

    Assert.assertFalse(batchSignatureVerifier.verify(batch, createSignature(batch, TestData.validCertificate)));
  }

  @Test
  public void testVerifyReturnsFalseWhenOriginsAreDifferent() throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("XYZ", "ABC"));

    DiagnosisKeyBatch modifiedBatch = batch.toBuilder().addKeys(
      batch.getKeysList().get(0).toBuilder().setOrigin(TestData.COUNTRY_A).build()).build();

    Assert.assertFalse(batchSignatureVerifier.verify(modifiedBatch, createSignature(modifiedBatch, TestData.validCertificate)));
  }

  @Test
  public void testVerifyReturnsFalseWhenSigningCertIsNotAddedToSignedData() throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("123456ABC"));

    Assert.assertFalse(batchSignatureVerifier.verify(batch, createSignatureWithoutCert(batch)));
  }

  @Test
  public void testVerifyReturnsFalseWhenSignerInfoIsNotAddedToSignedData()
    throws CertificateEncodingException, CMSException, IOException, OperatorCreationException {
    final DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("123456ABC"));

    Assert.assertFalse(batchSignatureVerifier.verify(batch, createSignatureWithoutSignerInfo(batch)));
  }

  @Test
  public void testVerifyReturnsFalseWhenPKCS7ContentTypeIsNotSignedData()
    throws CertificateEncodingException, CMSException, IOException, CertificateParsingException {
    final DiagnosisKeyBatch batch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("123456ABC"));
    Assert.assertFalse(batchSignatureVerifier.verify(batch, signatureGenerator.encryptData(new byte[]{41, 52, 38})));
  }

  private String createSignature(final DiagnosisKeyBatch batch, X509Certificate cert)
    throws CertificateEncodingException, OperatorCreationException, CMSException, IOException {
    final byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSignForDummyBatch(batch);
    return signatureGenerator.sign(bytesToSign, cert);
  }

  private String createSignatureWithoutCert(final DiagnosisKeyBatch batch)
    throws CertificateEncodingException, OperatorCreationException, CMSException, IOException {
    final byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSignForDummyBatch(batch);
    return signatureGenerator.createSignatureWithoutCert(bytesToSign);
  }

  private String createSignatureWithoutSignerInfo(final DiagnosisKeyBatch batch)
    throws CertificateEncodingException, CMSException, IOException, OperatorCreationException {
    final byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSignForDummyBatch(batch);
    return signatureGenerator.createSignatureWithoutSignerInfo(bytesToSign);
  }

  private String createSignatureWithCorruptedBatchBytes(final DiagnosisKeyBatch batch)
    throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSignForDummyBatch(batch);
    bytesToSign[0] = 0;
    bytesToSign[1] = 0;
    return signatureGenerator.sign(bytesToSign, TestData.validCertificate);
  }

  private String createSignatureWithCorruptedB64String(final DiagnosisKeyBatch batch)
    throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSignForDummyBatch(batch);
    final String base64Signature = signatureGenerator.sign(bytesToSign, TestData.validCertificate);
    return "XYZ" + base64Signature + "ABCD";
  }

  private String createInvalidSignatureForBatchWithIncorrectByteOder(final DiagnosisKeyBatch batch)
    throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {
    final byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSignWithIncorrectOrder(batch);
    return signatureGenerator.sign(bytesToSign, TestData.validCertificate);
  }

}
