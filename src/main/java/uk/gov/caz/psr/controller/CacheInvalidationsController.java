package uk.gov.caz.psr.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.service.CleanAirZoneService;

/**
 * Rest Controller with endpoints related to cache invalidations.
 */
@RestController
@AllArgsConstructor
public class CacheInvalidationsController implements CacheInvalidationsControllerApiSpec {

  public static final String CACHE_INVALIDATION_PATH = "/v1/cache-invalidations";

  private final CleanAirZoneService cleanAirZoneService;
  
  @Override
  public ResponseEntity<Void> cacheEvictCleanAirZones() {
    cleanAirZoneService.cacheEvictCleanAirZones();
    return ResponseEntity.accepted().build();
  }
  
}
