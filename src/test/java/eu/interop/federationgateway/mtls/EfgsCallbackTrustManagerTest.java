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

package eu.interop.federationgateway.mtls;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.service.CertificateService;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Optional;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

public class EfgsCallbackTrustManagerTest {

  private CertificateService certificateServiceMock;
  private EfgsCallbackTrustManager efgsCallbackTrustManager;

  @Before
  public void setUp() {
    certificateServiceMock = mock(CertificateService.class);

    efgsCallbackTrustManager = new EfgsCallbackTrustManager(certificateServiceMock);
  }

  @Test
  public void testTrustManagerShouldNotThrowAnException() throws CertificateException {
    X509Certificate certificateMock = mock(X509Certificate.class);

    when(certificateMock.getEncoded()).thenReturn(new byte[0]);

    X509Certificate[] certChain = {
      certificateMock,
      certificateMock,
      certificateMock
    };

    when(certificateServiceMock.getAuthenticationCertificate(anyString()))
      .thenReturn(Optional.empty())
      .thenReturn(Optional.of(new CertificateEntity()));

    efgsCallbackTrustManager.checkServerTrusted(certChain, "");

    verify(certificateServiceMock, times(2)).getAuthenticationCertificate(anyString());
  }

  @Test(expected = CertificateException.class)
  public void testTrustManagerShouldThrowExceptionIfCertIsNotFound() throws CertificateException {
    X509Certificate certificateMock = mock(X509Certificate.class);

    when(certificateMock.getEncoded()).thenReturn(new byte[0]);

    X509Certificate[] certChain = {
      certificateMock,
      certificateMock,
      certificateMock
    };

    when(certificateServiceMock.getAuthenticationCertificate(anyString()))
      .thenReturn(Optional.empty());

    efgsCallbackTrustManager.checkServerTrusted(certChain, "");

    verify(certificateServiceMock, times(3)).getAuthenticationCertificate(anyString());
  }

  @Test(expected = NotImplementedException.class)
  public void testTrustManagerThrowsNotImplementedWhenUsingClientVerifyMethod() throws CertificateException {
    efgsCallbackTrustManager.checkClientTrusted(new X509Certificate[0], "");
  }
}
