package uk.gov.caz.psr.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

public class AsyncOperationsMatcher {
  
  private Map<String, String> identifierDataMatch;
  
  public AsyncOperationsMatcher(List<String> data) {
    identifierDataMatch = new HashMap<String, String>();
    for (String datum : data) {
      identifierDataMatch.put(UUID.randomUUID().toString(), datum);
    }
  }
  
  public Set<Entry<String, String>> getEntrySet() {
    return identifierDataMatch.entrySet();
  }
  
  public String getVrnByIdentifier(String identifier) {
    return identifierDataMatch.get(identifier);
  }

}
