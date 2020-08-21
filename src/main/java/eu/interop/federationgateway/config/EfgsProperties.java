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

package eu.interop.federationgateway.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("efgs")
public class EfgsProperties {

  private final ContentNegotiation contentNegotiation = new ContentNegotiation();
  private final UploadSettings uploadSettings = new UploadSettings();
  private final DownloadSettings downloadSettings = new DownloadSettings();
  private final CertAuth certAuth = new CertAuth();
  private final Batching batching = new Batching();
  private final JndiResource jndiResource = new JndiResource();

  @Getter
  @Setter
  public static class ContentNegotiation {
    private String protobufVersion;
    private String jsonVersion;
  }

  @Getter
  @Setter
  public static class Batching {
    private int doclimit = 5000;
  }

  @Getter
  @Setter
  public static class UploadSettings {
    private int maximumUploadBatchSize;
  }

  @Getter
  @Setter
  public static class DownloadSettings {
    private int maxAgeInDays;
  }

  @Getter
  @Setter
  public static class JndiResource {
    private String resourceName;
  }


  @Getter
  @Setter
  public static class CertAuth {

    private final HeaderFields headerFields = new HeaderFields();
    private List<String> certWhitelist;

    @Getter
    @Setter
    public static class HeaderFields {
      private String thumbprint;
      private String distinguishedName;
    }
  }
}
