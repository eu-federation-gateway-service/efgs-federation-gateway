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

import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "callback_task")
public class CallbackTaskEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "created_at")
  private ZonedDateTime createdAt;

  @Column(name = "execution_lock")
  private ZonedDateTime executionLock;

  @Column(name = "last_try")
  private ZonedDateTime lastTry;

  @Column(name = "retries")
  private int retries;

  @OneToOne
  @JoinColumn(name = "not_before_id")
  private CallbackTaskEntity notBefore;

  @ManyToOne
  @JoinColumn(name = "batch_id")
  private DiagnosisKeyBatchEntity batch;

  @ManyToOne
  @JoinColumn(name = "subscription_id")
  private CallbackSubscriptionEntity callbackSubscription;
}
