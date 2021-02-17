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

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509KeyManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
public class ForceCertUsageX509KeyManager implements X509KeyManager {

  private final String[] dummyStringArray = new String[0];
  private final PrivateKey privateKey;
  private final X509Certificate x509Certificate;

  @Override
  public String[] getClientAliases(String keyType, Principal[] issuers) {
    return dummyStringArray;
  }

  @Override
  public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
    return StringUtils.EMPTY;
  }

  @Override
  public String[] getServerAliases(String keyType, Principal[] issuers) {
    return dummyStringArray;
  }

  @Override
  public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
    return StringUtils.EMPTY;
  }

  @Override
  public X509Certificate[] getCertificateChain(String alias) {
    return new X509Certificate[] { x509Certificate };
  }

  @Override
  public PrivateKey getPrivateKey(String alias) {
    return privateKey;
  }
}
