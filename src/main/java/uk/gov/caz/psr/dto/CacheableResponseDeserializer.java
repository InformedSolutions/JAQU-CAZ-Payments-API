package uk.gov.caz.psr.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class CacheableResponseDeserializer extends StdDeserializer<CacheableResponse> {

  /**
   * Default serializer id.
   */
  private static final long serialVersionUID = -8513897954051272367L;
  
  private ObjectMapper objectmapper;
  
  public CacheableResponseDeserializer() {
    super(CacheableResponse.class);
  }
  
  public CacheableResponseDeserializer(Class<CacheableResponse> t,
      ObjectMapper objectMapper) {
    super(t);
    this.objectmapper = objectMapper;
  }
  
  @Override
  public CacheableResponse deserialize(JsonParser p,
      DeserializationContext ctxt) throws IOException, JsonProcessingException {
    return this.objectmapper.readValue(ctxt.toString(), CacheableResponse.class);
  }

}
