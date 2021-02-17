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


  /**
   * The constructor for the audit-entries.
   *
   * @param country                   the country
   * @param uploadedTime              the time of the upload
   * @param uploaderThumbprint        the uploader-thumbprint
   * @param uploaderSigningThumbprint the thumbprint of the signing certificate
   * @param amount                    the amount of the uploaded keys
   * @param batchSignature            the batchsignature
   * @param minId                     Used by query - will not bestored in entity.
   */
  public AuditEntry(String country, ZonedDateTime uploadedTime, String uploaderThumbprint,
                    String uploaderSigningThumbprint, long amount, String batchSignature, Long minId) {
    this.country = country;
    this.uploadedTime = uploadedTime;
    this.uploaderThumbprint = uploaderThumbprint;
    this.uploaderSigningThumbprint = uploaderSigningThumbprint;
    this.amount = amount;
    this.batchSignature = batchSignature;
  }

  @Schema(example = "DE")
  private String country;

  @Schema(example = "2020-07-31T11:24:43.086Z")
  private ZonedDateTime uploadedTime;

  @Schema(example = "-----BEGIN CERTIFICATE-----\n"
    + "MIICyDCCAbCgAwIBAgIGAXR3DZUUMA0GCSqGSIb3DQEBBQUAMBwxCzAJBgNVBAYT\n"
    + "AkRFMQ0wCwYDVQQDDARkZW1vMB4XDTIwMDgyNzA4MDY1MloXDTIxMDkxMDA4MDY1\n"
    + "MlowHDELMAkGA1UEBhMCREUxDTALBgNVBAMMBGRlbW8wggEiMA0GCSqGSIb3DQEB\n"
    + "AQUAA4IBDwAwggEKAoIBAQCKR0TEJOO4z0ks4OMAovcyxuPpeZuR1JykNNFd3OR+\n"
    + "vFWJLJtDYgRjtuqSuKCghLa/ci+0yIs3OeitGtajqFIukYksvX2LxOZDYDUbnpGQ\n"
    + "DPNMVmpEavDBbvKON8C8K036pC41bNvwkTrfUyZ8iE+hV2+kj1SHUyw7jweEUoiw\n"
    + "NmMiaXXPiMIOj7D0qnmM+iTGN9g/DrJ/IvvsgiGpK3QlQ5pnHs2BvzrSw4LFAZ8c\n"
    + "SQfWKheZVHfQf26mJFdEzowrzfzForDdeFAPIIirhufE3jWFxj1thfztu+VSMj84\n"
    + "sDqodEt2VJOY+DvLB1Ls/26LSmFtMnCEuBAhkbQ1E0tbAgMBAAGjEDAOMAwGA1Ud\n"
    + "EwEB/wQCMAAwDQYJKoZIhvcNAQEFBQADggEBABaMEQz4Gbj+G0SZGZaIDoUFDB6n\n"
    + "1R6iUS0zTBgsV8pSpFhwPryRiLdeNzIzsDdQ1ack1NfQ6YPn3/yOJ/SvnXs6n+vO\n"
    + "WQW2KsuiymPSd/wjeywRRMfCysHjrmE+m+8lrFDrKuPnrACwQIsX9PDEsRRBnpSy\n"
    + "5NKUZn6u3iPV9x6rwYCdCa/8VDGLqVb3eEE5dbFaYG9uW02cSbmsiZm8KmW8b6BF\n"
    + "eIwHVRAH6Cs1VZI8UIrdVGCE111tUo/0957rF+/doFyJcwX+4ESH0m2MsHFjXDfG\n"
    + "U8yTjiUh/b2Erk4TCmrJpux30QRhsNZwkmEYSbRv+vp5/obgH1mL5ouoV5I=\n"
    + "-----END CERTIFICATE-----\n")
  private String uploaderCertificate;

  @Schema(example = "69c697c045b4cdaa441a28af0ec1cc4128153b9ddc796b66bfa04b02ea3e103e")
  private String uploaderThumbprint;

  @Schema(example = "KfyyJxglK3ol/ckgqbcZKhpazYlNAfQd/hdrBNGuTTFkFwaUYvbA4ydPOu6SXZuyqhUuLsfdK6dDVaETumMZHVLT0R"
    + "/YWTsAFSAk/hmJPv+KLh1rk4+BwhfRc7E7Y7MH1JFFMuRH+hAVjMEO8LtPLmb6yMXXe"
    + "+8CTTwaPG5HCVnGNsvrVuRbQekRRULmftRWyeEFxuv8CTikFG3CVl5bbB"
    + "FdkHkcdrED8kg7AnYyML315iijWKH14iWrcwiuBYyIYvGxArhufuyyJf"
    + "VO/bpcVvFgGrEo8SpxPJhtWRi1xLnjKHJQKdrV7dS9dD5OQHcpFQtIp7pZ9SRB3FqcCcfMMg==")
  private String uploaderOperatorSignature;

  @Schema(example = "-----BEGIN CERTIFICATE-----\n"
    + "MIICyDCCAbCgAwIBAgIGAXR3DdOUMA0GCSqGSIb3DQEBBQUAMBwxCzAJBgNVBAYT\n"
    + "AkRFMQ0wCwYDVQQDDARkZW1vMB4XDTIwMDgyNzA4MDcwOFoXDTIxMDkxMDA4MDcw\n"
    + "OFowHDELMAkGA1UEBhMCREUxDTALBgNVBAMMBGRlbW8wggEiMA0GCSqGSIb3DQEB\n"
    + "AQUAA4IBDwAwggEKAoIBAQCKR0TEJOO4z0ks4OMAovcyxuPpeZuR1JykNNFd3OR+\n"
    + "vFWJLJtDYgRjtuqSuKCghLa/ci+0yIs3OeitGtajqFIukYksvX2LxOZDYDUbnpGQ\n"
    + "DPNMVmpEavDBbvKON8C8K036pC41bNvwkTrfUyZ8iE+hV2+kj1SHUyw7jweEUoiw\n"
    + "NmMiaXXPiMIOj7D0qnmM+iTGN9g/DrJ/IvvsgiGpK3QlQ5pnHs2BvzrSw4LFAZ8c\n"
    + "SQfWKheZVHfQf26mJFdEzowrzfzForDdeFAPIIirhufE3jWFxj1thfztu+VSMj84\n"
    + "sDqodEt2VJOY+DvLB1Ls/26LSmFtMnCEuBAhkbQ1E0tbAgMBAAGjEDAOMAwGA1Ud\n"
    + "EwEB/wQCMAAwDQYJKoZIhvcNAQEFBQADggEBABISpoT/FgaCMlV0zXVq+HrHgcgl\n"
    + "GSm3OQfgG1cY+YnkFY+vngdxZutJAWdCaEPmX2xBHQGp0VW7Sd6ueNpOekSZT15N\n"
    + "3ZKhYc7Lqn4Ra/VkgRoOYZbmalp61unrS9AjPrlGu9/vXjLUEJOc3Qm8na3MFWgl\n"
    + "hs1tOZW+CwIJM9yWRh5VmTBDIcWj/cbjizAoLEetIPeD2RiP6k1YSZ0prDPP9zGg\n"
    + "JHOmNJWHTWsi6jx3Ipqm55iq2uBpasxoOBS1zAbb86vKni4R1nDAVK1MqTVHc0CD\n"
    + "uGc+5KKdtbio8/zueC+PI5nN5JckuBwkOu3LYs4s6GyGNYM0zbtnqiWST0Y=\n"
    + "-----END CERTIFICATE-----\n")
  private String signingCertificate;

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
