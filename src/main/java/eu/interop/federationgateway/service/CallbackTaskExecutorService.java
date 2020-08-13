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

package eu.interop.federationgateway.service;

import eu.interop.federationgateway.config.EfgsProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.ProxyProvider;

@Component
@Slf4j
public class CallbackTaskExecutorService {

  private final EfgsProperties efgsProperties;

  private final WebClient webClient;

  private final CallbackTaskService callbackTaskService;

  public CallbackTaskExecutorService(EfgsProperties efgsProperties, CallbackTaskService callbackTaskService) {
    this.efgsProperties = efgsProperties;
    this.callbackTaskService = callbackTaskService;

    HttpClient httpClient = HttpClient.create()
      .tcpConfiguration(tcpClient -> {
          // configure timeout for connection
          tcpClient = tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
            efgsProperties.getCallback().getTimeout());

          // configure timeout for answer
          tcpClient = tcpClient.doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(
            efgsProperties.getCallback().getTimeout(), TimeUnit.MILLISECONDS)));

          // configure proxy
          tcpClient = tcpClient.proxy(proxy ->
            proxy
              .type(ProxyProvider.Proxy.HTTP)
              .host(efgsProperties.getCallback().getProxyHost())
              .port(efgsProperties.getCallback().getProxyPort())
          );
          return tcpClient;
        }
      );

    this.webClient = WebClient.builder()
      .clientConnector(new ReactorClientHttpConnector(httpClient))
      .build();
  }

 /* @RequiredArgsConstructor
  private class CallbackTask implements Runnable {

    private final String url;
    private final String country;
    private final String certHash;
    private int retries = 0;

    @Override
    public void run() {
      //log.info("Starting callback.\", country={}, batchTag=\"{}", country, callbackEvent.getBatchTag());

      webClient.get()
        .uri(URI.create(url))
        .header(efgsProperties.getCertAuth().getHeaderFields().getThumbprint(), certHash)
        //.attribute("batchTag", callbackEvent.getBatchTag())
        //.attribute("date", callbackEvent.getDate())
        .exchange()
        .subscribe(
          response -> {
            log.info("Got Response for callback\", country={}, status=\"{}", country, response.rawStatusCode());
          },
          exception -> {
            log.error("Failure on Callback\", error= \"{}\", country={}", exception.getMessage(), country);

            if (retries <= efgsProperties.getCallback().getMaxRetries()) {
              retries++;

              log.info("Scheduling retry of callback\", error= \"{}\", country={}, retry=\"{}",
                exception.getMessage(), country, retries);

              //executor.schedule(this, efgsProperties.getCallback().getRetryWait(), TimeUnit.MILLISECONDS);
            } else {
              log.error("Reached limit for callback retries.\", country={}, retry=\"{}", country, retries);
            }
          });
    }
  }*/
}
