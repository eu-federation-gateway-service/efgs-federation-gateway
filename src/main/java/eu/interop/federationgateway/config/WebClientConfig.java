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

import eu.interop.federationgateway.mtls.EfgsCallbackTrustManager;
import eu.interop.federationgateway.mtls.ForceCertUsageX509KeyManager;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

  private final EfgsProperties efgsProperties;

  private final KeyStore callbackKeyStore;

  private final EfgsCallbackTrustManager efgsCallbackTrustManager;

  /**
   * Configures WebClient for HTTP requests for callback feature.
   *
   * @return Instance of WebClient
   * @throws UnrecoverableKeyException if the key cannot be recovered
   * @throws NoSuchAlgorithmException if the algorithm for recovering the key cannot be found
   * @throws KeyStoreException if the keystore has not been initialized
   * @throws SSLException if the SSL context cannot be build
   */
  @Bean
  public WebClient webClient() throws UnrecoverableKeyException, NoSuchAlgorithmException,
          KeyStoreException, SSLException {

    PrivateKey privateKey = (PrivateKey) callbackKeyStore.getKey(
      efgsProperties.getCallback().getKeyStorePrivateKeyAlias(),
      efgsProperties.getCallback().getKeyStorePass().toCharArray()
    );

    X509Certificate certificate = (X509Certificate) callbackKeyStore.getCertificate(
      efgsProperties.getCallback().getKeyStoreCertificateAlias()
    );

    SslContext sslContext = SslContextBuilder
      .forClient()
      .enableOcsp(false)
      .keyManager(new ForceCertUsageX509KeyManager(privateKey, certificate))
      .trustManager(efgsCallbackTrustManager)
      .build();

    HttpClient httpClient = HttpClient.create()
      .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext))
      .tcpConfiguration(tcpClient -> {
        // configure timeout for connection
        tcpClient = tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
          efgsProperties.getCallback().getTimeout());

        // configure timeout for answer
        tcpClient = tcpClient.doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(
          efgsProperties.getCallback().getTimeout(), TimeUnit.MILLISECONDS)));

        // configure proxy
        if (efgsProperties.getCallback().getProxyHost() != null
          && !efgsProperties.getCallback().getProxyHost().isEmpty()) {
          tcpClient = tcpClient.proxy(proxy ->
            proxy
              .type(ProxyProvider.Proxy.HTTP)
              .host(efgsProperties.getCallback().getProxyHost())
              .port(efgsProperties.getCallback().getProxyPort())
              .username(efgsProperties.getCallback().getProxyUser())
              .password(s -> efgsProperties.getCallback().getProxyPassword())
          );
        }
        return tcpClient;
      });

    return WebClient.builder()
      .clientConnector(new ReactorClientHttpConnector(httpClient))
      .build();
  }
}
