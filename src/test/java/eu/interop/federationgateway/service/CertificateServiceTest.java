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

package eu.interop.federationgateway.service;

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.repository.CertificateRepository;
import eu.interop.federationgateway.testconfig.EfgsTestKeyStore;
import eu.interop.federationgateway.utils.CertificateUtils;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = EfgsTestKeyStore.class)
public class CertificateServiceTest {

  @Autowired
  CertificateRepository certificateRepository;

  @Autowired
  CertificateService certificateService;

  @Before
  public void setup() throws CertificateException, SignatureException, NoSuchAlgorithmException, IOException, OperatorCreationException, InvalidKeyException {
    certificateRepository.deleteAll();
    TestData.insertCertificatesForAuthentication(certificateRepository);
  }

  @Test
  public void certificateRepositoryShouldReturnCertificate() {
    Optional<CertificateEntity> certOptional = certificateService.getCertificate(TestData.validCertificateHash, TestData.AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.SIGNING);

    Assert.assertTrue(certOptional.isPresent());
    Assert.assertEquals(TestData.validCertificateHash, certOptional.get().getThumbprint());

    String authCertThumbprint = CertificateUtils.getCertThumbprint(TestData.validAuthenticationCertificate);
    certOptional = certificateService.getAuthenticationCertificate(authCertThumbprint);

    Assert.assertTrue(certOptional.isPresent());
    Assert.assertEquals(authCertThumbprint, certOptional.get().getThumbprint());
  }

  @Test
  public void certificateRepositoryShouldNotReturnCertificateIfIntegrityOfRawDataIsViolated() {
    Optional<CertificateEntity> certOptional = certificateService.getCertificate(TestData.validCertificateHash, TestData.AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.SIGNING);
    Optional<CertificateEntity> anotherCertOptional = certificateService.getCertificate(
      CertificateUtils.getCertThumbprint(TestData.notValidYetCertificate), TestData.AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.SIGNING);

    Assert.assertTrue(certOptional.isPresent());
    Assert.assertTrue(anotherCertOptional.isPresent());

    CertificateEntity cert = certOptional.get();
    cert.setRawData(anotherCertOptional.get().getRawData());

    certificateRepository.save(cert);

    certOptional = certificateService.getCertificate(TestData.validCertificateHash, TestData.AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.SIGNING);
    Assert.assertTrue(certOptional.isEmpty());
  }

  @Test
  public void certificateRepositoryShouldNotReturnCertificateIfIntegrityOfSignatureIsViolated() {
    Optional<CertificateEntity> certOptional = certificateService.getCertificate(TestData.validCertificateHash, TestData.AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.SIGNING);
    Optional<CertificateEntity> anotherCertOptional = certificateService.getCertificate(
      CertificateUtils.getCertThumbprint(TestData.notValidYetCertificate), TestData.AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.SIGNING);

    Assert.assertTrue(certOptional.isPresent());
    Assert.assertTrue(anotherCertOptional.isPresent());

    CertificateEntity cert = certOptional.get();
    cert.setSignature(anotherCertOptional.get().getSignature());

    certificateRepository.save(cert);

    certOptional = certificateService.getCertificate(TestData.validCertificateHash, TestData.AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.SIGNING);
    Assert.assertTrue(certOptional.isEmpty());
  }

  @Test
  public void certificateRepositoryShouldNotReturnCertificateIfIntegrityOfThumbprintIsViolated() {
    Optional<CertificateEntity> certOptional = certificateService.getCertificate(TestData.validCertificateHash, TestData.AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.SIGNING);
    Optional<CertificateEntity> anotherCertOptional = certificateService.getCertificate(
      CertificateUtils.getCertThumbprint(TestData.notValidYetCertificate), TestData.AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.SIGNING);

    Assert.assertTrue(certOptional.isPresent());
    Assert.assertTrue(anotherCertOptional.isPresent());

    certificateRepository.delete(anotherCertOptional.get());

    CertificateEntity cert = certOptional.get();
    cert.setThumbprint(anotherCertOptional.get().getThumbprint());

    certificateRepository.save(cert);

    certOptional = certificateService.getCertificate(anotherCertOptional.get().getThumbprint(), TestData.AUTH_CERT_COUNTRY, CertificateEntity.CertificateType.SIGNING);
    Assert.assertTrue(certOptional.isEmpty());
  }
}
