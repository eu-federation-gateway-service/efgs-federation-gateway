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

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.repository.CertificateRepository;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

public class SignatureGenerator {

  public SignatureGenerator(CertificateRepository certificateRepository) throws CertificateException,
    NoSuchAlgorithmException, IOException, OperatorCreationException, InvalidKeyException, SignatureException {
    TestData.insertCertificatesForAuthentication(certificateRepository);
  }

  public String sign(final byte[] data, X509Certificate cert) throws CertificateEncodingException,
    OperatorCreationException, CMSException, IOException {
    return sign(data, cert, true, true);
  }

  public String createSignatureWithoutCert(final byte[] data)
    throws CertificateEncodingException, OperatorCreationException, CMSException, IOException {
    return sign(data, TestData.validCertificate, false, true);
  }

  public String createSignatureWithoutSignerInfo(final byte[] data)
    throws CertificateEncodingException, CMSException, IOException, OperatorCreationException {
    return sign(data, TestData.validCertificate, true, false);
  }

  private String sign(final byte[] data, X509Certificate cert, final boolean certMustBeAdded,
                      final boolean signerInfoMustBeAdded)
    throws CertificateEncodingException, OperatorCreationException, IOException, CMSException {
    final CMSSignedDataGenerator signedDataGenerator = new CMSSignedDataGenerator();
    if (signerInfoMustBeAdded) {
      signedDataGenerator.addSignerInfoGenerator(createSignerInfo(cert));
    }
    if (certMustBeAdded) {
      signedDataGenerator.addCertificate(createCertificateHolder(cert));
    }
    CMSSignedData singedData = signedDataGenerator.generate(new CMSProcessableByteArray(data), false);
    return Base64.getEncoder().encodeToString(singedData.getEncoded());
  }

  public String encryptData(final byte[] data) throws CertificateEncodingException, CMSException, IOException {
    final CMSEnvelopedDataGenerator cmsEnvelopedDataGenerator = new CMSEnvelopedDataGenerator();
    cmsEnvelopedDataGenerator.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(TestData.validCertificate));
    final CMSTypedData msg = new CMSProcessableByteArray(data);
    final OutputEncryptor encryptor = new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES128_CBC).build();
    final CMSEnvelopedData cmsEnvelopedData = cmsEnvelopedDataGenerator.generate(msg, encryptor);
    return Base64.getEncoder().encodeToString(cmsEnvelopedData.getEncoded());
  }

  private SignerInfoGenerator createSignerInfo(X509Certificate cert) throws OperatorCreationException,
    CertificateEncodingException {
    return new JcaSignerInfoGeneratorBuilder(createDigestBuilder()).build(createContentSigner(cert), cert);
  }

  private X509CertificateHolder createCertificateHolder(X509Certificate cert) throws CertificateEncodingException,
    IOException {
    return new X509CertificateHolder(cert.getEncoded());
  }

  private DigestCalculatorProvider createDigestBuilder() throws OperatorCreationException {
    return new JcaDigestCalculatorProviderBuilder().build();

  }

  private ContentSigner createContentSigner(X509Certificate cert) throws OperatorCreationException {
    return new JcaContentSignerBuilder(cert.getSigAlgName()).build(TestData.keyPair.getPrivate());
  }
}
