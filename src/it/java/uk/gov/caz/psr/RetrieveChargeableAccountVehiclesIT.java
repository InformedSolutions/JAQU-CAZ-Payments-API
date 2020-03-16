package uk.gov.caz.psr;

import io.restassured.RestAssured;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
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

  @Autowired
  private DataSource dataSource;
  
  @LocalServerPort
  int randomServerPort;

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    chargeableVrns = new ArrayList<>();
  }
  
  @AfterEach
  public void clearDatabase() {
    executeSqlFrom("data/sql/clear-all-caz-entrant-payments.sql");   
  }
  
  @Test
  public void shouldReturnHttpOkWhenValidRequestWithAllChargeableVehicles() 
      throws JsonProcessingException {
    
    List<String> vrns = mockFullNextPageOfChargeableVrns(CLEAN_AIR_ZONE_ID);
    
    givenAssertion()
      .forAccountId(ACCOUNT_ID)
      .forPageSize("2")
      .forVrn("ABC123")
      .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
      .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .responseContainsExpectedData(vrns.subList(0, 2), vrns.get(0), 
          vrns.get(1));
  }


  @Test
  public void shouldReturnHttpOkWhenValidRequestWithMixedVehicles() 
      throws JsonProcessingException {
    mockAccountServiceCursorCall(
        ACCOUNT_ID, "ABC123", NEXT_CURSOR_RESPONSE);
    mockAccountServiceCursorCall(
        ACCOUNT_ID, "LMN234", EMPTY_CURSOR_RESPONSE);
    mockAccountVehiclesAndMakeHalfChargeable(NEXT_CURSOR_RESPONSE);
    
    givenAssertion()
      .forAccountId(ACCOUNT_ID)
      .forDirection("next")
      .forPageSize("10")
      .forVrn("ABC123")
      .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
      .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .responseContainsExpectedData(chargeableVrns, chargeableVrns.get(0), null);
  }
  
  @Test
  public void shouldReturnHttpOkWhenValidRequestWithUnprocessableVrn() 
      throws JsonProcessingException {
    List<String> vrns = mockFullNextPageOfChargeableVrnsWithUnprocessableVrn(CLEAN_AIR_ZONE_ID);
    
    givenAssertion()
      .forAccountId(ACCOUNT_ID)
      .forDirection("next")
      .forPageSize("10")
      .forVrn("ABC123")
      .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
      .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .responseContainsExpectedData(vrns.subList(0, vrns.size() - 1), vrns.get(0), null);
  }
  
  @Test
  public void shouldReturnHttpOkWhenValidRequestWithoutVrnOrDirection() 
      throws JsonProcessingException {
    mockAccountServiceCursorCallWithoutCursorParameter(ACCOUNT_ID, 
        NEXT_CURSOR_RESPONSE);
    List<String> vrns = mockFullNextPageOfChargeableVrns(CLEAN_AIR_ZONE_ID);
    
    givenAssertion()
      .forAccountId(ACCOUNT_ID)
      .forPageSize("10")
      .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
      .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .responseContainsExpectedData(vrns, vrns.get(0), null);
  }
  
  @Test
  public void shouldReturnHttpOkAndNoFirstVrnWhenNoFurtherPreviousVrnsFound() 
      throws JsonProcessingException {
    mockAccountServiceCursorCall(
        ACCOUNT_ID, "LMN234", PREVIOUS_CURSOR_RESPONSE);
    mockAccountVehiclesAndMakeHalfChargeable(PREVIOUS_CURSOR_RESPONSE);
    
    Collections.sort(chargeableVrns);
    
    givenAssertion()
      .forAccountId(ACCOUNT_ID)
      .forDirection("previous")
      .forPageSize("10")
      .forVrn("LMN234")
      .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
      .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .responseContainsExpectedData(chargeableVrns, null, 
          chargeableVrns.get(chargeableVrns.size() - 1));
  }

  @Test
  public void shouldReturnHttpOkWhenEntrantPaymentsExist() throws JsonProcessingException {
    executeSqlFrom("data/sql/add-entrant-payments.sql");
    List<String> vrns = mockFullNextPageOfChargeableVrns("4dc6ea23-77d3-4bfe-8180-7662c33f88ad");
    
    givenAssertion()
      .forAccountId(ACCOUNT_ID)
      .forPageSize("10")
      .forVrn("ABC123")
      .forCleanAirZoneId("4dc6ea23-77d3-4bfe-8180-7662c33f88ad")
      .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
      .then()
      .responseIsReturnedWithHttpOkStatusCode()
      .responseContainsExpectedDataWithEntrantPayments(vrns, vrns.get(0), null);
    
  }
  
  @ParameterizedTest
  @CsvSource({",,,", "1,,10,", "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,,,",
    "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,,0,","4dc6ea23-77d3-4bfe-8180-7662c33f88ad,,one,",
    "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,test,10,", "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,previous,10,"})
  public void shouldReturnHttpBadRequestWhenInvalidQueryStringsSupplied(String cleanAirZoneId,
      String direction, String pageSize, String vrn) throws JsonProcessingException {
    mockAccountServiceCursorCallWithoutCursorParameter(ACCOUNT_ID, NEXT_CURSOR_RESPONSE);
    givenAssertion()
    .forAccountId(ACCOUNT_ID)
    .forPageSize(pageSize)
    .forDirection(direction)
    .forVrn(vrn)
    .forCleanAirZoneId(cleanAirZoneId)
    .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
    .then()
    .responseIsReturnedWithStatusCode(400);   
  }
  
  private List<String> getAccountVehicleVrnsFromFile(String fileName) throws JsonProcessingException {
    return new ObjectMapper().readValue(readJson(fileName), 
        new TypeReference<List<String>>() {});
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion givenAssertion() {
    return new RetrieveChargeableAccountVehiclesJourneyAssertion();
  }
  
  private void mockAccountVehiclesAndMakeHalfChargeable(String responseFile) 
      throws JsonProcessingException {
    List<String> vrns = getAccountVehicleVrnsFromFile(responseFile);
    List<String> responses = Arrays.asList(new String[]{
        "vehicle-compliance-response-single-zone.json",
        "vehicle-compliance-compliant-response-single-zone.json"});
    mockVccsBulkComplianceCallWithMixedResults(vrns, CLEAN_AIR_ZONE_ID, responses, 200);
  }
  
  private List<String> mockFullNextPageOfChargeableVrns(String cleanAirZoneId) 
      throws JsonProcessingException {
    mockAccountServiceCursorCall(ACCOUNT_ID, "ABC123", NEXT_CURSOR_RESPONSE);
    List<String> vrns = getAccountVehicleVrnsFromFile(NEXT_CURSOR_RESPONSE);
    mockVccsBulkComplianceCall(vrns, cleanAirZoneId, 
        "vehicle-compliance-response-single-zone.json", 200);
    return vrns;
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }
  
  private List<String> mockFullNextPageOfChargeableVrnsWithUnprocessableVrn(String cleanAirZoneId) 
      throws JsonProcessingException {
    mockAccountServiceCursorCall(ACCOUNT_ID, "ABC123", NEXT_CURSOR_RESPONSE);
    List<String> vrns = getAccountVehicleVrnsFromFile(NEXT_CURSOR_RESPONSE);
    mockVccsBulkComplianceCallWithUnknownVrn(vrns, cleanAirZoneId, 
        "vehicle-compliance-response-single-zone.json", 200);
    return vrns;
  }
}
