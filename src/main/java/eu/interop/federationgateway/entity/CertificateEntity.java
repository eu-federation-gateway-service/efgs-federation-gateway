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

import java.io.Serializable;
import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class represents the Certificate - entity to store certificate information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "certificate")
public class CertificateEntity implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "created_at")
  private ZonedDateTime createdAt;

  @Column(name = "thumbprint", unique = true)
  private String thumbprint;

  @Column(name = "country")
  private String country;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private CertificateType type;

  @Column(name = "revoked")
  private Boolean revoked;

  @Column(name = "host")
  private String host;

  @Column(name = "signature")
  private String signature;

  @Column(name = "rawData")
  private String rawData;

  public enum CertificateType {
    AUTHENTICATION,
    SIGNING,
    CALLBACK
  }
}


