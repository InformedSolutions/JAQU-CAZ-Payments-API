package uk.gov.caz.psr;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.controller.CacheInvalidationsController;

@IntegrationTest
@AutoConfigureMockMvc
public class CacheInvalidationIT extends ExternalCallsIT {

  private static final UUID ANY_CORRELATION_ID = UUID.randomUUID();
  
  private ClientAndServer mockServer;
  
  @Autowired
  private MockMvc mockMvc;
  
  private static final String CLEAN_AIR_ZONE_CACHE_INVALIDATION_PATH =
      CacheInvalidationsController.CACHE_INVALIDATION_PATH + "/clean-air-zones";
  
  @BeforeEach
  public void startMockServer() {
    mockServer = startClientAndServer(1080);
  }
  
  @AfterEach
  public void stopMockServer() {
    mockServer.stop();
  }
  
  @Test
  void canInvalidateCleanAirZonesCache() throws Exception {
    mockMvc
    .perform(post(CLEAN_AIR_ZONE_CACHE_INVALIDATION_PATH)
        .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .accept(MediaType.APPLICATION_JSON))
    .andExpect(status().is2xxSuccessful());
  }
}
