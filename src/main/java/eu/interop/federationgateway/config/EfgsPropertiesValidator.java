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

import javax.annotation.PostConstruct;
import javax.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EfgsPropertiesValidator {

  private final EfgsProperties efgsProperties;

  /**
   * A set of checks whether the provided configuration is valid.
   */
  @PostConstruct
  public void validateSettings() {
    if (efgsProperties.getUploadSettings().getMaximumUploadBatchSize() > efgsProperties.getBatching().getDoclimit()) {
      throw new ValidationException(
        "Invalid Application Configuration: Maximum upload size must be less or smaller then batch size!");
    }

    log.info("Validated Application Configuration.");
  }

}
