package eu.interop.federationgateway.config;

import eu.interop.federationgateway.dbencryption.DbEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class DbEncryption {
  @Value("${efgs.dbencryption.password:}")
  private String dbEncryptionPassword;

  @Bean
  public DbEncryptionService dbEncryptionService() {
    return DbEncryptionService.getInstance(dbEncryptionPassword);
  }
}
