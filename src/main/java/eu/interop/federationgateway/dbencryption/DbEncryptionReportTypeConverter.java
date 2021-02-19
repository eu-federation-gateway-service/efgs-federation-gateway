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

package eu.interop.federationgateway.dbencryption;

import eu.interop.federationgateway.entity.DiagnosisKeyPayload;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.persistence.AttributeConverter;
import javax.persistence.PersistenceException;

public class DbEncryptionReportTypeConverter implements AttributeConverter<DiagnosisKeyPayload.ReportType, String> {

  @Override
  public String convertToDatabaseColumn(DiagnosisKeyPayload.ReportType s) {
    try {
      return DbEncryptionService.getInstance().encryptString(s.name());
    } catch (InvalidAlgorithmParameterException | InvalidKeyException 
            | BadPaddingException | IllegalBlockSizeException e) {
      throw new PersistenceException(e);
    }
  }

  @Override
  public DiagnosisKeyPayload.ReportType convertToEntityAttribute(String s) {
    try {
      return DiagnosisKeyPayload.ReportType.valueOf(DbEncryptionService.getInstance().decryptString(s));
    } catch (InvalidAlgorithmParameterException | InvalidKeyException 
            | BadPaddingException | IllegalBlockSizeException e) {
      throw new PersistenceException(e);
    }
  }

}
