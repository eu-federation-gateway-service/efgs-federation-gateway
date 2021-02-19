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

package eu.interop.federationgateway.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EfgsKeyStore {

  private final EfgsProperties efgsProperties;

  /**
   * Creates a KeyStore instance with keys for EFGS TrustAnchor.
   *
   * @return KeyStore Instance
   * @throws KeyStoreException if no implementation for the specified type found
   * @throws IOException if there is an I/O or format problem with the keystore data
   * @throws CertificateException if any of the certificates in the keystore could not be loaded
   * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
   */
  @Bean
  public KeyStore trustAnchorKeyStore() throws KeyStoreException, IOException,
    CertificateException, NoSuchAlgorithmException {
    KeyStore keyStore = KeyStore.getInstance("JKS");

    loadKeyStore(
      keyStore,
      efgsProperties.getTrustAnchor().getKeyStorePath(),
      efgsProperties.getTrustAnchor().getKeyStorePass().toCharArray());

    return keyStore;
  }

  /**
   * Creates a KeyStore instance with keys for EFGS Callback feature.
   *
   * @return KeyStore Instance
   * @throws KeyStoreException if no implementation for the specified type found
   * @throws IOException if there is an I/O or format problem with the keystore data
   * @throws CertificateException if any of the certificates in the keystore could not be loaded
   * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
   */
  @Bean
  public KeyStore callbackKeyStore() throws KeyStoreException, IOException,
    CertificateException, NoSuchAlgorithmException {
    KeyStore keyStore = KeyStore.getInstance("JKS");

    loadKeyStore(
      keyStore,
      efgsProperties.getCallback().getKeyStorePath(),
      efgsProperties.getCallback().getKeyStorePass().toCharArray());

    return keyStore;
  }

  private void loadKeyStore(KeyStore keyStore, String path, char[] password)
    throws CertificateException, NoSuchAlgorithmException, IOException {

    InputStream fileStream;

    if (path.startsWith("classpath:")) {
      String resourcePath = path.substring(10);
      fileStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    } else {
      File file = new File(path);
      fileStream = file.exists() ? getStream(path) : null;
    }

    if (fileStream != null && fileStream.available() > 0) {
      keyStore.load(fileStream, password);
      fileStream.close();
    } else {
      keyStore.load(null);
      log.info("Could not find Keystore {}", path);
    }

  }

  private InputStream getStream(String path) {
    try {
      return new FileInputStream(path);
    } catch (IOException e) {
      log.info("Could not find Keystore {}", path);
    }
    return null;
  }
}
