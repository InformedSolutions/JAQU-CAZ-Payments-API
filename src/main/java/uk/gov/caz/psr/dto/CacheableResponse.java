package uk.gov.caz.psr.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class CacheableResponse<T> implements Serializable {

  /**
   * Serialization ID.
   */
  private static final long serialVersionUID = 8132305942474588935L;

  public int code;
  public T body;

}
