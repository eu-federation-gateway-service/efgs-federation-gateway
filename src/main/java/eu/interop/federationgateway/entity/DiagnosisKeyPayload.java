/*-
 * ---license-start
 * EU-Federation-Gateway-Service / efgs-federation-gateway
 * ---
 * Copyright (C) 2020 T-Systems International GmbH and all other contributors
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

package eu.interop.federationgateway.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Embeddable
public class DiagnosisKeyPayload implements Serializable {

  @Column(name = "payload_keyData")
  private byte[] keyData;

  @Column(name = "payload_rollingStartIntervalNumber")
  private int rollingStartIntervalNumber;

  @Column(name = "payload_rollingPeriod")
  private int rollingPeriod;

  @Column(name = "payload_transmissionRiskLevel")
  private int transmissionRiskLevel;

  @Column(name = "payload_visitedCountries")
  private String visitedCountries;

  @Column(name = "payload_origin")
  private String origin;

  @Column(name = "payload_reportType")
  private ReportType reportType;

  @Column(name = "payload_daysSinceOnsetOfSymptoms")
  private int daysSinceOnsetOfSymptoms;

  public enum ReportType {
    UNKNOWN,
    CONFIRMED_TEST,
    CONFIRMED_CLINICAL_DIAGNOSIS,
    SELF_REPORT,
    RECURSIVE,
    REVOKED,
  }

}
