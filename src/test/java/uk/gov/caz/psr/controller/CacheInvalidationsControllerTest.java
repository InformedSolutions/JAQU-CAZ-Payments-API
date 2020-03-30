package uk.gov.caz.psr.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.service.CleanAirZoneService;

@ExtendWith(MockitoExtension.class)
class CacheInvalidationsControllerTest {

  @Mock
  private CleanAirZoneService cleanAirZoneService;
  
  @InjectMocks
  private CacheInvalidationsController cacheInvalidationsController;

  @Test
  public void shouldCacheEvictForCleanAirZones() {
    // when
    cacheInvalidationsController.cacheEvictCleanAirZones();

    // then
    verify(cleanAirZoneService, times(1)).cacheEvictCleanAirZones();
  }
}