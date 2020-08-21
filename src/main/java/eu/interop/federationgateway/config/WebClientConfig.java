package eu.interop.federationgateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.ProxyProvider;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

  private final EfgsProperties efgsProperties;

  /**
   * Configures WebClient for HTTP requests for callback feature.
   *
   * @return Instance of WebClient
   */
  @Bean
  public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
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
            );
          }
          return tcpClient;
        }
      );

    return WebClient.builder()
      .clientConnector(new ReactorClientHttpConnector(httpClient))
      .build();
  }

}
