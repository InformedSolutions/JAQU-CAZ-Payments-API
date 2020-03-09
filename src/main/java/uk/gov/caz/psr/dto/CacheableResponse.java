package uk.gov.caz.psr.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import retrofit2.Response;

@Builder
@JsonSerialize(using = CacheableResponseSerializer.class)
@JsonDeserialize(using = CacheableResponseDeserializer.class)
@Value
@Getter
public class CacheableResponse<T> implements Serializable {
  
  /**
   * Serialization ID.
   */
  private static final long serialVersionUID = 8132305942474588935L;

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
  public Response<T> response;
  
}
