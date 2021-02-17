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

package eu.interop.federationgateway.config;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import java.io.IOException;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

public class ProtobufConverter extends JsonFormat {

  @Override
  public void printField(Descriptors.FieldDescriptor field, Object value, JsonGenerator generator) throws IOException {
    if (field.getType() == Descriptors.FieldDescriptor.Type.BYTES && !field.isRepeated()) {
      generator.print("\"" + field.getName() + "\": \"");

      ByteString byteString = (ByteString) value;
      generator.print(Base64.getEncoder().encodeToString(byteString.toByteArray()));

      generator.print("\"");
    } else {
      super.printField(field, value, generator);
    }
  }

  @Override
  protected void mergeField(Tokenizer tokenizer, ExtensionRegistry extensionRegistry, Message.Builder builder)
    throws ParseException {

    Descriptors.Descriptor type = builder.getDescriptorForType();
    String name = tokenizer.currentToken().replaceAll("\"|'", "");
    Descriptors.FieldDescriptor field = type.findFieldByName(name);

    if (field != null && field.getType() == Descriptors.FieldDescriptor.Type.BYTES) {
      tokenizer.consumeIdentifier();
      tokenizer.consume(":");

      try {
        byte[] value = Base64.getDecoder().decode(tokenizer.consumeString());
        builder.setField(field, ByteString.copyFrom(value));
      } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Base64 in JSON object");
      }

      if (tokenizer.tryConsume(",")) {
        mergeField(tokenizer, extensionRegistry, builder);
      }

    } else {
      super.mergeField(tokenizer, extensionRegistry, builder);
    }
  }
}
