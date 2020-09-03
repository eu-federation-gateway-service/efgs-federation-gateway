package eu.interop.federationgateway.config;

import static net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider.Configuration.builder;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ShedLockConfig {

  /**
   * Creates a LockProvider for ShedLock.
   *
   * @param dataSource JPA datasource
   * @return LockProvider
   */
  @Bean
  public LockProvider lockProvider(DataSource dataSource) {
    return new JdbcTemplateLockProvider(builder()
      .withTableName("shedlock")
      .withJdbcTemplate(new JdbcTemplate(dataSource))
      .usingDbTime()
      .build()
    );
  }

}
