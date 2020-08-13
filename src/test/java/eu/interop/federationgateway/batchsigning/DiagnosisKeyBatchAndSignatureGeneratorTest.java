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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.List;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DiagnosisKeyBatchAndSignatureGeneratorTest {

  @Autowired
  CertificateRepository certificateRepository;

  SignatureGenerator signatureGenerator;

  @Before
  public void setup() throws NoSuchAlgorithmException, CertificateException, CertIOException, OperatorCreationException {
    signatureGenerator = new SignatureGenerator(certificateRepository);
  }

  @Test
  public void test() throws OperatorCreationException, CertificateEncodingException, CMSException, IOException {

    final DiagnosisKeyBatch diagnosisKeyBatch = BatchSignatureUtilsTest.createDiagnosisKeyBatch(List.of("123", "456", "abc"));
    final byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSignForDummyBatch(diagnosisKeyBatch);
    final String signature = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    FileOutputStream batchOutputStream = null;
    FileOutputStream signatureOutputStream = null;
    try {
      batchOutputStream = new FileOutputStream("diagnosisKeyBatchFiveKeys.bin");
      signatureOutputStream = new FileOutputStream("diagnosisKeyBatchFiveKeysPKCS7.b64");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    try {
      diagnosisKeyBatch.writeTo(batchOutputStream);
      signatureOutputStream.write(signature.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }

  }


}
