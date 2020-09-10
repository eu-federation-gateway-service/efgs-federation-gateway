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

package eu.interop.federationgateway.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(
  description = "Entity representation per country in audit results."
)
public class AuditEntry {

  @Schema(example = "DE")
  private String country;

  @Schema(example = "2020-07-31T11:24:43.086Z")
  private ZonedDateTime uploadedTime;

  @Schema(example = "69c697c045b4cdaa441a28af0ec1cc4128153b9ddc796b66bfa04b02ea3e103e")
  private String uploaderThumbprint;

  @Schema(example = "KfyyJxglK3ol/ckgqbcZKhpazYlNAfQd/hdrBNGuTTFkFwaUYvbA4ydPOu6SXZuyqhUuLsfdK6dDVaETumMZHVLT0R"
    + "/YWTsAFSAk/hmJPv+KLh1rk4+BwhfRc7E7Y7MH1JFFMuRH+hAVjMEO8LtPLmb6yMXXe"
    + "+8CTTwaPG5HCVnGNsvrVuRbQekRRULmftRWyeEFxuv8CTikFG3CVl5bbB"
    + "FdkHkcdrED8kg7AnYyML315iijWKH14iWrcwiuBYyIYvGxArhufuyyJf"
    + "VO/bpcVvFgGrEo8SpxPJhtWRi1xLnjKHJQKdrV7dS9dD5OQHcpFQtIp7pZ9SRB3FqcCcfMMg==")
  private String uploaderOperatorSignature;

  @Schema(example = "69c697c045b4cdaa441a28af0ec1bb4128153b9ddc796b66bfa04b02ea3e103e")
  private String uploaderSigningThumbprint;

  @Schema(example = "o53CbAa77LyIMFc5Gz+B2Jc275Gdg/SdLayw7gx0GrTcinR95zfTLr8nNHgJMYlX3rD8Y11zB/Osyt0VLCcDZr"
    + "+e6gtMms8qr0qMzw1G74cSPiKCb6TEpc/pBBGxljtOfinvksLjJzW3Pu4fbKz6KikdUjXA8lxYx//aosd7qWxo2lnxbJlo1URXw/BINanoKj"
    + "+RZSSCheZzi8dbUjmfOP8IZUFvtpf3isyMpaD+5+gpcGgNqNz9aUPvwk++jjTlKj+e4ZtFkUh0nPR4hYsmXct9jn32lk2M3r3CcmvgwvW"
    + "+VIrYRGSEmgjGy2EwzvA5nVhsaA+/udnmbyQw9LjAOQ==")
  private String signingCertificateOperatorSignature;

  @Schema(example = "3")
  private long amount;

  @Schema(example = "exampleBatchSignature")
  private String batchSignature;

}
