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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SemVerUtils {

  /**
   * Checks whether the two provided version strings are compatible regarding the following rules:
   * Major version MUST match. Minor version of check version must be less or equal to minor base version.
   *
   * @param baseVersion  The base version number which is used as reference.
   * @param checkVersion The version number which needs to be checked.
   * @return true if both versions are compatible.
   * @throws SemVerParsingException is thrown if an error occours during parsing.
   */
  public static boolean parseSemVerAndCheckCompatibility(
    String baseVersion, String checkVersion) throws SemVerParsingException {
    SemVer parsedBaseVersion = parseSemVer(baseVersion);
    SemVer parsedCheckVersion = parseSemVer(checkVersion);

    return parsedBaseVersion.major == parsedCheckVersion.major && parsedCheckVersion.minor == parsedBaseVersion.minor;
  }

  /**
   * Parses a string for major and minor version information.
   *
   * @param input version string
   * @return parsed version information
   * @throws SemVerParsingException if string is not properly formatted
   */
  public static SemVer parseSemVer(String input) throws SemVerParsingException {
    String[] splitted = input.split("\\.");

    if (splitted.length != 2) {
      throw new SemVerParsingException("Version can just contain a major and minor version splitted by a dot!");
    }

    try {
      return new SemVer(
        Integer.parseInt(splitted[0]),
        Integer.parseInt(splitted[1])
      );
    } catch (NumberFormatException e) {
      throw new SemVerParsingException("Given version string has an invalid number format!");
    }
  }

  public static class SemVerParsingException extends Exception {
    private static final long serialVersionUID = 1L;
    
    SemVerParsingException(String message) {
      super(message);
    }
  }

  @AllArgsConstructor
  @Getter
  public static class SemVer {
    int major;
    int minor;
  }
}
