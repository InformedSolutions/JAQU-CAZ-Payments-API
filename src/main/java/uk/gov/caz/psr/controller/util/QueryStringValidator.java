package uk.gov.caz.psr.controller.util;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;

@Component
@Slf4j
public class QueryStringValidator {

  /**
   * Ensures a given map contains the correct keys, with correctly formatted values.
   * @param map the map to check
   * @param requiredStringParams required keys in the map that contain String values
   * @param requiredNumericalParams required keys in the map that contain numerical values
   */
  public void validateRequest(Map<String, String> map, List<String> requiredStringParams,
      List<String> requiredNumericalParams) {
    for (String param : requiredStringParams) {
      if (queryStringInvalid(param, map)) {
        throw new InvalidRequestPayloadException("Incorrect parameters supplied");
      }
    }

    if (requiredNumericalParams != null) {
      for (String param : requiredNumericalParams) {
        if (numericalQueryStringInvalid(param, map)) {
          throw new InvalidRequestPayloadException("Incorrect parameters supplied");
        }
      }
    }
  }
  
  private Boolean queryStringInvalid(String key, Map<String, String> map) {
    return !map.containsKey(key) || !StringUtils.hasText(map.get(key));
  }
  
  private Boolean numericalQueryStringInvalid(String key, Map<String, String> map) {
    // query string invalid if not in map, if empty or if less than 0
    if (queryStringInvalid(key, map)) { 
      return true;
    }
    
    try {
      return Integer.parseInt(map.get(key)) < 0;
    } catch (Exception e) {
      log.info("Parameter {} was not a number", key);
      return true;
    }
  }
}
