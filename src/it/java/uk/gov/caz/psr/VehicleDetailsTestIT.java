package uk.gov.caz.psr;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import uk.gov.caz.psr.controller.PaymentsController;

@IntegrationTest
@AutoConfigureMockMvc
class VehicleDetailsTestIT extends ExternalCallsIT {

  private ClientAndServer mockServer;
  
  @Autowired
  private MockMvc mockMvc;
  
  private static final String GET_VEHICLE_DETAILS_PATH =
      PaymentsController.BASE_PATH + "/"
          + PaymentsController.GET_VEHICLE_DETAILS;
  
  @BeforeEach
  public void startMockServer() {
    mockServer = startClientAndServer(1080);
  }
  
  @AfterEach
  public void stopMockServer() {
    mockServer.stop();
  }
  
  @Test
  public void canFetchVehicleDetails( ) throws Exception {
    mockVccsVehicleDetailsCall();
    String testVrn = "TESTVRN";
    mockMvc
      .perform(get(GET_VEHICLE_DETAILS_PATH.replace("{vrn}", testVrn))
            .header(X_CORRELATION_ID_HEADER, UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status()
            .is2xxSuccessful());    
  }

}
