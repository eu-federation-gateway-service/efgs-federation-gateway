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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.persistence.AttributeConverter;
import javax.persistence.PersistenceException;

public class DbEncryptionByteArrayConverter implements AttributeConverter<byte[], String> {

  @Override
  public String convertToDatabaseColumn(byte[] s) {
    try {
      return DbEncryptionService.getInstance().encryptByteArray(s);
    } catch (InvalidAlgorithmParameterException | InvalidKeyException 
            | BadPaddingException | IllegalBlockSizeException e) {
      throw new PersistenceException(e);
    }
  }

  @Override
  public byte[] convertToEntityAttribute(String s) {
    try {
      return DbEncryptionService.getInstance().decryptByteArray(s);
    } catch (InvalidAlgorithmParameterException | InvalidKeyException 
            | BadPaddingException | IllegalBlockSizeException e) {
      throw new PersistenceException(e);
    }
  }

}
