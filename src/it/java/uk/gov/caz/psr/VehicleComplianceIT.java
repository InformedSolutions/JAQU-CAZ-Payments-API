package uk.gov.caz.psr;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
public class VehicleComplianceIT extends ExternalCallsIT {

  private static ClientAndServer mockServer;
  
  @Autowired
  private MockMvc mockMvc;
  
  private static final String GET_VEHICLE_COMPLIANCE_PATH =
      PaymentsController.BASE_PATH + "/"
          + PaymentsController.GET_COMPLIANCE;
 
  private static final String GET_UNRECOGNISED_VEHICLE_COMPLIANCE_PATH =
      PaymentsController.BASE_PATH + "/"
          + PaymentsController.GET_UNRECOGNISED_VEHICLE_COMPLIANCE;
 
  
  @BeforeAll
  public static void startMockServer() {
    mockServer = startClientAndServer(1080);
  }
  
  @AfterAll
  public static void stopMockServer() {
    mockServer.stop();
  }

  @AfterEach
  public void resetMockServer() {
    mockServer.reset();
  }
  
  @Test
  void canFetchVehicleCompliance() throws Exception {
    
    String testVrn = "TESTVRN";
    String zone = UUID.randomUUID().toString();
    mockVccsComplianceCall(testVrn, zone, "vehicle-compliance-response.json", 200);
    
    mockMvc
    .perform(get(GET_VEHICLE_COMPLIANCE_PATH.replace("{vrn}", testVrn))
        .header(X_CORRELATION_ID_HEADER, UUID.randomUUID())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .param("zones", zone))
        .andExpect(status()
        .is2xxSuccessful());
  }
  
  @Test
  void canFetchUnrecognisedVehicleCompliance() throws Exception {
    
    String type = "PRIVATE_CAR";
    String zone = UUID.randomUUID().toString();
    mockVccsUnknownVehicleComplianceCall(type, zone, "unknown-vehicle-compliance-response.json", 200);
    
    mockMvc
    .perform(get(GET_UNRECOGNISED_VEHICLE_COMPLIANCE_PATH.replace("{type}", type))
        .header(X_CORRELATION_ID_HEADER, UUID.randomUUID())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .param("zones", zone))
        .andExpect(status()
        .is2xxSuccessful());
  }

}
