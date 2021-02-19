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

package eu.interop.federationgateway.testdata;

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.batchsigning.BatchSignatureUtilsTest;
import eu.interop.federationgateway.batchsigning.SignatureGenerator;
import eu.interop.federationgateway.model.EfgsProto;
import eu.interop.federationgateway.repository.CertificateRepository;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestDataGenerator {

  @Autowired
  CertificateRepository certificateRepository;

  SignatureGenerator signatureGenerator;

  @Before
  public void setup() throws OperatorCreationException, CertificateException, IOException, NoSuchAlgorithmException,
    InvalidKeyException, SignatureException, KeyStoreException {
    TestData.insertCertificatesForAuthentication(certificateRepository);
    signatureGenerator = new SignatureGenerator(certificateRepository);
  }

  /**
   * Testcase to create some example batch data with signing keys to demonstrate functionality of
   * EFGS with a simple REST client like Postman.
   * <p>
   * This Testcase is ignored by default. To create testdata remove the @Ignore annotation temporary.
   * @throws java.io.IOException
   * @throws java.security.cert.CertificateEncodingException
   * @throws org.bouncycastle.operator.OperatorCreationException
   * @throws org.bouncycastle.cms.CMSException
   */
  @Test
  @Ignore("see description above")
  public void createTestDataForManualTesting() throws IOException, CertificateEncodingException,
    OperatorCreationException, CMSException {
    if (Files.isDirectory(Path.of("testdata"))) {
      Files.deleteIfExists(Path.of("testdata/batch1_3keys.bin"));
      Files.deleteIfExists(Path.of("testdata/batch2_3keys.bin"));
      Files.deleteIfExists(Path.of("testdata/batch3_2keys.bin"));
      Files.deleteIfExists(Path.of("testdata/batch4_2keys.bin"));
    } else {
      Files.createDirectory(Path.of("testdata"));
    }

    FileOutputStream outputStreamBatch1 = new FileOutputStream("testdata/batch1_3keys.bin", false);
    FileOutputStream outputStreamBatch2 = new FileOutputStream("testdata/batch2_3keys.bin", false);
    FileOutputStream outputStreamBatch3 = new FileOutputStream("testdata/batch3_2keys.bin", false);
    FileOutputStream outputStreamBatch4 = new FileOutputStream("testdata/batch4_2keys.bin", false);

    FileOutputStream outputStreamCertificate = new FileOutputStream("testdata/cert.pem", false);

    FileOutputStream outputStreamCertData = new FileOutputStream("testdata/signatures.txt");

    Assert.assertTrue(outputStreamBatch1.getFD().valid());

    EfgsProto.DiagnosisKeyBatch batch1 = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addKeys(TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(10).build())
      .addKeys(TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(20).build())
      .addKeys(TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(30).build())
      .build();

    EfgsProto.DiagnosisKeyBatch batch2 = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addKeys(TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(40).build())
      .addKeys(TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(50).build())
      .addKeys(TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(60).build())
      .build();

    EfgsProto.DiagnosisKeyBatch batch3 = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addKeys(TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(70).build())
      .addKeys(TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(80).build())
      .build();

    EfgsProto.DiagnosisKeyBatch batch4 = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addKeys(TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(90).build())
      .addKeys(TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(100).build())
      .build();

    outputStreamBatch1.write(batch1.toByteArray());
    outputStreamBatch1.close();
    outputStreamBatch2.write(batch2.toByteArray());
    outputStreamBatch2.close();
    outputStreamBatch3.write(batch3.toByteArray());
    outputStreamBatch3.close();
    outputStreamBatch4.write(batch4.toByteArray());
    outputStreamBatch4.close();

    OutputStreamWriter certFileOutputStreamWriter = new OutputStreamWriter(outputStreamCertData);
    certFileOutputStreamWriter.write("Signing Cert Hash: ");
    certFileOutputStreamWriter.write(TestData.validCertificateHash);
    certFileOutputStreamWriter.write("\n\n");

    certFileOutputStreamWriter.write("batch1_3keys.bin signature: ");
    certFileOutputStreamWriter.write(signatureGenerator.sign(BatchSignatureUtilsTest.createBytesToSign(batch1),
      TestData.validCertificate));
    certFileOutputStreamWriter.write("\n\n");

    certFileOutputStreamWriter.write("batch2_3keys.bin signature: ");
    certFileOutputStreamWriter.write(signatureGenerator.sign(BatchSignatureUtilsTest.createBytesToSign(batch2),
      TestData.validCertificate));
    certFileOutputStreamWriter.write("\n\n");

    certFileOutputStreamWriter.write("batch3_2keys.bin signature: ");
    certFileOutputStreamWriter.write(signatureGenerator.sign(BatchSignatureUtilsTest.createBytesToSign(batch3),
      TestData.validCertificate));
    certFileOutputStreamWriter.write("\n\n");

    certFileOutputStreamWriter.write("batch4_2keys.bin signature: ");
    certFileOutputStreamWriter.write(signatureGenerator.sign(BatchSignatureUtilsTest.createBytesToSign(batch4),
      TestData.validCertificate));
    certFileOutputStreamWriter.write("\n");

    certFileOutputStreamWriter.close();
    outputStreamCertData.close();

    OutputStreamWriter outputStreamWriterCertificate = new OutputStreamWriter(outputStreamCertificate);
    JcaPEMWriter privateKeyWriter = new JcaPEMWriter(outputStreamWriterCertificate);
    privateKeyWriter.writeObject(TestData.keyPair.getPrivate());
    privateKeyWriter.writeObject(TestData.validCertificate);
    privateKeyWriter.close();
    outputStreamWriterCertificate.close();
    outputStreamCertificate.close();
  }

}
