package eu.interop.federationgateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.naming.NamingException;
import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "eu.interop.federationgateway.repository")
public class PersistenceJNDIConfig {
  @Autowired
  EfgsProperties efgsProperties;


  @Bean
  public DataSource dataSource() throws NamingException {
    return (DataSource) new JndiTemplate().lookup(efgsProperties.getJndiResource().getResourceName());
  }

}
