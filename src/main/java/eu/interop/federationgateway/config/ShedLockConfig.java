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
