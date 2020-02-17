package uk.gov.caz.psr.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

/**
 * Utilty class to build a hashmap against a list of data to
 * give each datum a unique key.
 */
public class AsyncOperationsMatcher {
  
  private Map<String, String> identifierDataMatch;
  
  /**
   * Build new matcher map from list of data.
   * @param data a list to generate keys for
   */
  public AsyncOperationsMatcher(List<String> data) {
    identifierDataMatch = new HashMap<String, String>();
    for (String datum : data) {
      identifierDataMatch.put(UUID.randomUUID().toString(), datum);
    }
  }
  
  /**
   * Extract entry set from map.
   * @return entry set
   */
  public Set<Entry<String, String>> getEntrySet() {
    return identifierDataMatch.entrySet();
  }
  
  /**
   * Extract value from map with key.
   * @param identifier the key to use
   * @return the value associated with the identifier
   */
  public String getValueByKey(String identifier) {
    return identifierDataMatch.get(identifier);
  }

}
