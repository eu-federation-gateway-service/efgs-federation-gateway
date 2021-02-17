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

package eu.interop.federationgateway.entity;

import eu.interop.federationgateway.dbencryption.DbEncryptionByteArrayConverter;
import eu.interop.federationgateway.dbencryption.DbEncryptionIntConverter;
import eu.interop.federationgateway.dbencryption.DbEncryptionReportTypeConverter;
import eu.interop.federationgateway.dbencryption.DbEncryptionStringConverter;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Convert;
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
  @Convert(converter = DbEncryptionByteArrayConverter.class)
  private byte[] keyData;

  @Column(name = "payload_rollingStartIntervalNumber")
  @Convert(converter = DbEncryptionIntConverter.class)
  private int rollingStartIntervalNumber;

  @Column(name = "payload_rollingPeriod")
  @Convert(converter = DbEncryptionIntConverter.class)
  private int rollingPeriod;

  @Column(name = "payload_transmissionRiskLevel")
  @Convert(converter = DbEncryptionIntConverter.class)
  private int transmissionRiskLevel;

  @Column(name = "payload_visitedCountries")
  @Convert(converter = DbEncryptionStringConverter.class)
  private String visitedCountries;

  @Column(name = "payload_origin")
  @Convert(converter = DbEncryptionStringConverter.class)
  private String origin;

  @Column(name = "payload_reportType")
  @Convert(converter = DbEncryptionReportTypeConverter.class)
  private ReportType reportType;

  @Column(name = "payload_daysSinceOnsetOfSymptoms")
  @Convert(converter = DbEncryptionIntConverter.class)
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
