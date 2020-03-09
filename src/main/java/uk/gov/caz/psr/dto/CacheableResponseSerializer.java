package uk.gov.caz.psr.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class CacheableResponseSerializer
    extends StdSerializer<CacheableResponse> {

  private ObjectMapper objectmapper;

  public CacheableResponseSerializer() {
    super(CacheableResponse.class);
    this.objectmapper = new ObjectMapper();
  }

  public CacheableResponseSerializer(Class<CacheableResponse> t,
      ObjectMapper objectMapper) {
    super(t);
    this.objectmapper = new ObjectMapper();
  }

  @Override
  public void serialize(CacheableResponse value, JsonGenerator gen,
      SerializerProvider provider) throws IOException {
    gen.writeStartObject();
    gen.writeStringField("response", objectmapper.writeValueAsString(value.getResponse()));
    gen.writeEndObject();
  }

}
