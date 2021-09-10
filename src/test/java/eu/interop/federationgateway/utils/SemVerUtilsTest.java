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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SemVerUtilsTest {

  @Test
  public void testParsing() throws SemVerUtils.SemVerParsingException {
    SemVerUtils.SemVer result = SemVerUtils.parseSemVer("1.0");

    Assertions.assertEquals(1, result.getMajor());
    Assertions.assertEquals(0, result.getMinor());
  }

  @Test
  public void testCompatibility() throws SemVerUtils.SemVerParsingException {
    Assertions.assertTrue(SemVerUtils.parseSemVerAndCheckCompatibility("1.0", "1.0"));
    Assertions.assertTrue(SemVerUtils.parseSemVerAndCheckCompatibility("8.5", "8.5"));
    Assertions.assertFalse(SemVerUtils.parseSemVerAndCheckCompatibility("0.1", "1.0"));
    Assertions.assertFalse(SemVerUtils.parseSemVerAndCheckCompatibility("1.1", "0.0"));
    Assertions.assertFalse(SemVerUtils.parseSemVerAndCheckCompatibility("1.5", "1.1"));
  }

  @Test
  public void testParsingErrorTooMuchEntries() {
    Assertions.assertThrows(SemVerUtils.SemVerParsingException.class, () -> SemVerUtils.parseSemVer("1.2.3.4"));
  }

  @Test
  public void testParsingErrorInvalidString() {
    Assertions.assertThrows(SemVerUtils.SemVerParsingException.class, () -> SemVerUtils.parseSemVer("nonSem.VerString"));
  }
}
