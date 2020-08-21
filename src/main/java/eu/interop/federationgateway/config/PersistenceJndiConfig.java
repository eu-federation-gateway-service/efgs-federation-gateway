package eu.interop.federationgateway.config;

import javax.naming.NamingException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@Profile("!test")
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "eu.interop.federationgateway.repository")
public class PersistenceJndiConfig {

  @Autowired
  EfgsProperties efgsProperties;


  @Bean
  public DataSource dataSource() throws NamingException {

    return (DataSource) new JndiTemplate().lookup(efgsProperties.getJndiResource().getResourceName());
  }

}
