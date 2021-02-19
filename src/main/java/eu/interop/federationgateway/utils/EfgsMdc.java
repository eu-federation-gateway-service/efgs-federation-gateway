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

package eu.interop.federationgateway.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

/**
 * Wrapper for MDC to escape values for better log files.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EfgsMdc {

  /**
   * Put a diagnostic context value (the <code>value</code> parameter) as identified with the
   * <code>key</code> parameter into the current thread's diagnostic context map. The
   * <code>key</code> parameter cannot be null. The <code>value</code> parameter
   * can be null only if the underlying implementation supports it.
   *
   * <p>This method delegates all work to the MDC of the underlying logging system.
   *
   * @param key   non-null key
   * @param value value to put in the map
   * @throws IllegalArgumentException in case the "key" parameter is null
   */
  public static void put(String key, String value) {
    value = value == null ? ">>null<<" : value;

    value = value.replace("\"", "'");
    MDC.put(key, "\"" + value + "\"");
  }

  /**
   * Put a diagnostic context value (the <code>value</code> parameter) as identified with the
   * <code>key</code> parameter into the current thread's diagnostic context map. The
   * <code>key</code> parameter cannot be null. The <code>value</code> parameter
   * can be null only if the underlying implementation supports it.
   *
   * <p>This method delegates all work to the MDC of the underlying logging system.
   *
   * @param key   non-null key
   * @param value a numeric value to put in the map
   * @throws IllegalArgumentException in case the "key" parameter is null
   */
  public static void put(String key, long value) {
    put(key, String.valueOf(value));
  }

  /**
   * Put a diagnostic context value (the <code>value</code> parameter) as identified with the
   * <code>key</code> parameter into the current thread's diagnostic context map. The
   * <code>key</code> parameter cannot be null. The <code>value</code> parameter
   * can be null only if the underlying implementation supports it.
   *
   * <p>This method delegates all work to the MDC of the underlying logging system.
   *
   * @param key   non-null key
   * @param value a date value to put in the map
   * @throws IllegalArgumentException in case the "key" parameter is null
   */
  public static void put(String key, Date value) {
    ZonedDateTime timestamp = ZonedDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC);

    put(
      key,
      timestamp.format(DateTimeFormatter.ISO_INSTANT)
    );
  }

  /**
   * Remove the diagnostic context identified by the <code>key</code> parameter using
   * the underlying system's MDC implementation. The <code>key</code> parameter
   * cannot be null. This method does nothing if there is no previous value
   * associated with <code>key</code>.
   *
   * @param key non-null key
   * @throws IllegalArgumentException in case the "key" parameter is null
   */
  public static void remove(String key) {
    MDC.remove(key);
  }

  /**
   * Clear all entries in the MDC of the underlying implementation.
   */
  public static void clear() {
    MDC.clear();
  }
}
