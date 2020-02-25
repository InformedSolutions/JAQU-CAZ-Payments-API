package uk.gov.caz.psr;

import io.restassured.RestAssured;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.LocalServerPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.caz.psr.ExternalCallsIT;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.journeys.RetrieveChargeableAccountVehiclesJourneyAssertion;

@FullyRunningServerIntegrationTest
public class RetrieveChargeableAccountVehiclesIT extends ExternalCallsIT {

  private static final String CLEAN_AIR_ZONE_ID =
      "5cd7441d-766f-48ff-b8ad-1809586fea37";
  private static final String ACCOUNT_ID = UUID.randomUUID().toString();
  private static final String NEXT_CURSOR_RESPONSE = "account-vehicles-cursor-response.json";
  private static final String PREVIOUS_CURSOR_RESPONSE = "account-vehicles-cursor-previous-response.json";
  private static final String EMPTY_CURSOR_RESPONSE = "account-vehicles-cursor-empty-response.json";

  @LocalServerPort
  int randomServerPort;

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
  }
  
  @Test
  public void shouldReturnHttpOkWhenValidRequestWithAllChargeableVehicles() 
      throws JsonProcessingException {
    mockAccountServiceChargeableVehiclesCall(ACCOUNT_ID, 
        "ABC123", NEXT_CURSOR_RESPONSE);
    List<String> vrns = getAccountVehicleVrnsFromFile(NEXT_CURSOR_RESPONSE);
    for (String vrn : vrns) {
      mockVccsComplianceCall(vrn, "vehicle-compliance-response-single-zone.json", 200);
    }
    
    givenAssertion()
      .forAccountId(ACCOUNT_ID)
      .forPageSize("10")
      .forVrn("ABC123")
      .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
      .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .responseContainsExpectedData(vrns, vrns.get(0), vrns.get(vrns.size()-1));
  }
  
  @Test
  public void shouldReturnHttpOkWhenValidRequestWithMixedVehicles() 
      throws JsonProcessingException {
    mockAccountServiceChargeableVehiclesCall(
        ACCOUNT_ID, "ABC123", NEXT_CURSOR_RESPONSE);
    List<String> vrns = getAccountVehicleVrnsFromFile(NEXT_CURSOR_RESPONSE);
    List<String> chargeable = new ArrayList<String>();
    for (int i = 0; i < vrns.size(); i++) {
      // whatever is called next, return an empty response
      mockAccountServiceChargeableVehiclesCall(
          ACCOUNT_ID, vrns.get(i), EMPTY_CURSOR_RESPONSE);
      if (i % 2 == 0) {
        mockVccsComplianceCall(vrns.get(i), 
            "vehicle-compliance-response-single-zone.json", 200);
        chargeable.add(vrns.get(i));
      } else {
        mockVccsComplianceCall(vrns.get(i), 
            "vehicle-compliance-compliant-response-single-zone.json", 200);        
      }
    }
    
    givenAssertion()
      .forAccountId(ACCOUNT_ID)
      .forDirection("next")
      .forPageSize("10")
      .forVrn("ABC123")
      .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
      .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .responseContainsExpectedData(chargeable, vrns.get(0), null);
  }
  
  @Test
  public void shouldReturnHttpOkWhenValidRequestWithUnprocessableVrn() 
      throws JsonProcessingException {
    mockAccountServiceChargeableVehiclesCall(ACCOUNT_ID, 
        "ABC123", NEXT_CURSOR_RESPONSE);
    List<String> vrns = getAccountVehicleVrnsFromFile(NEXT_CURSOR_RESPONSE);
    for (int i = 0; i < vrns.size() - 1; i++) {
      mockVccsComplianceCall(vrns.get(i), "vehicle-compliance-response-single-zone.json", 200);
    }
    mockVccsUnprocessableEntityComplianceCall(vrns.get(vrns.size() - 1));
    
    givenAssertion()
      .forAccountId(ACCOUNT_ID)
      .forDirection("next")
      .forPageSize("10")
      .forVrn("ABC123")
      .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
      .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .responseContainsExpectedData(vrns, vrns.get(0), vrns.get(vrns.size() - 1));
  }
  
  @Test
  public void shouldReturnHttpOkWhenValidRequestWithoutVrnOrDirection() 
      throws JsonProcessingException {
    mockAccountServiceChargeableVehiclesCallWithoutCursor(ACCOUNT_ID, 
        NEXT_CURSOR_RESPONSE);
    List<String> vrns = getAccountVehicleVrnsFromFile(NEXT_CURSOR_RESPONSE);
    for (String vrn : vrns) {
      mockVccsComplianceCall(vrn, "vehicle-compliance-response-single-zone.json", 200);
    }
    
    givenAssertion()
      .forAccountId(ACCOUNT_ID)
      .forPageSize("10")
      .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
      .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .responseContainsExpectedData(vrns, vrns.get(0), vrns.get(vrns.size() - 1));
  }
  
  @Test
  public void shouldReturnHttpOkAndNoFirstVrnWhenNoFurtherPreviousVrnsFound() 
      throws JsonProcessingException {
    mockAccountServiceChargeableVehiclesCall(
        ACCOUNT_ID, "LMN234", PREVIOUS_CURSOR_RESPONSE);
    mockAccountServiceChargeableVehiclesCall(
        ACCOUNT_ID, "ABC123", EMPTY_CURSOR_RESPONSE);
    List<String> vrns = getAccountVehicleVrnsFromFile(PREVIOUS_CURSOR_RESPONSE);
    List<String> chargeable = new ArrayList<String>();
    for (int i = 0; i < vrns.size(); i++) {
      if (i % 2 == 0) {
        mockVccsComplianceCall(vrns.get(i), 
            "vehicle-compliance-response-single-zone.json", 200);
        chargeable.add(vrns.get(i));
      } else {
        mockVccsComplianceCall(vrns.get(i), 
            "vehicle-compliance-compliant-response-single-zone.json", 200);        
      }
    }
    Collections.sort(chargeable);
    
    givenAssertion()
      .forAccountId(ACCOUNT_ID)
      .forDirection("previous")
      .forPageSize("10")
      .forVrn("LMN234")
      .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
      .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .responseContainsExpectedData(chargeable, null, chargeable.get(chargeable.size() - 1));
  }
  
  private List<String> getAccountVehicleVrnsFromFile(String fileName) throws JsonProcessingException {
    return new ObjectMapper().readValue(readJson(fileName), 
        new TypeReference<List<String>>() {});
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion givenAssertion() {
    return new RetrieveChargeableAccountVehiclesJourneyAssertion();
  }
  
}
