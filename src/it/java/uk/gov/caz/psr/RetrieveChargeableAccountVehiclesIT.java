package uk.gov.caz.psr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
import uk.gov.caz.definitions.dto.accounts.ChargeableVehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.journeys.RetrieveChargeableAccountVehiclesJourneyAssertion;

@FullyRunningServerIntegrationTest
public class RetrieveChargeableAccountVehiclesIT extends ExternalCallsIT {

  private static final String CLEAN_AIR_ZONE_ID = "5cd7441d-766f-48ff-b8ad-1809586fea37";
  private static final String ACCOUNT_ID = UUID.randomUUID().toString();
  private static final String NEXT_CURSOR_RESPONSE = "account-vehicles-cursor-response.json";
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
  public void shouldReturnHttpOkWhenValidRequest()
      throws JsonProcessingException {

    List<String> vrns = mockChargeableVehiclesWithCursor();

    givenAssertion()
        .forAccountId(ACCOUNT_ID)
        .forPageSize("10")
        .forVrn("ABC123")
        .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
        .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
        .then()
        .responseIsReturnedWithHttpOkStatusCode()
        .responseContainsExpectedData(vrns, vrns.get(0), null);
  }

  @Test
  public void shouldReturnHttpOkWhenValidRequestWithoutVrnOrDirection()
      throws JsonProcessingException {
    List<String> vrns = mockChargeableVehiclesWithoutCursor();

    givenAssertion()
        .forAccountId(ACCOUNT_ID)
        .forPageSize("10")
        .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
        .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
        .then()
        .responseIsReturnedWithHttpOkStatusCode()
        .responseContainsExpectedData(vrns, null, null);
  }

  @Test
  public void shouldReturnHttpOkWhenEntrantPaymentsExist() throws JsonProcessingException {
    executeSqlFrom("data/sql/add-entrant-payments.sql");
    List<String> vrns = mockChargeableVehiclesWithCursor();

    givenAssertion()
        .forAccountId(ACCOUNT_ID)
        .forPageSize("3")
        .forVrn("ABC123")
        .forCleanAirZoneId("4dc6ea23-77d3-4bfe-8180-7662c33f88ad")
        .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
        .then()
        .responseIsReturnedWithHttpOkStatusCode()
        .responseContainsExpectedDataWithEntrantPayments(vrns.subList(0, 3), vrns.get(0),
            vrns.get(2));
  }

  @Test
  public void shouldReturnHttpOkAndEmptyArrayWhenNoVrnsCanBeFound() {
    mockEmptyChargeableVehiclesWithCursor();

    givenAssertion()
        .forAccountId(ACCOUNT_ID)
        .forDirection("next")
        .forPageSize("10")
        .forVrn("ABC123")
        .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
        .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
        .then()
        .responseIsReturnedWithHttpOkStatusCode()
        .responseContainsExpectedData(Collections.emptyList(), null, null);
  }


  @ParameterizedTest
  @CsvSource({",,,", "1,,10,", "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,,,",
      "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,,-1,", "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,,0,",
      "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,,one,", "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,test,10,",
      "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,previous,10,"})
  public void shouldReturnHttpBadRequestWhenInvalidQueryStringsSupplied(String cleanAirZoneId,
      String direction, String pageSize, String vrn) {
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

  @Test
  public void shouldReturnHttpBadRequestWhenNoQueryStringsProvided() {
    givenAssertion()
        .forAccountId(ACCOUNT_ID)
        .whenRequestIsMadeToRetrieveChargeableAccountVehiclesWithoutQueryStrings()
        .then()
        .responseIsReturnedWithStatusCode(400);
  }

  @Test
  public void shouldReturnHttpNotFoundWhenAccountIdCannotBeFound() {
    String invalidAccountId = UUID.randomUUID().toString();
    mockAccountServiceCursorCallWithError(invalidAccountId, "ABC123", 404);
    givenAssertion()
        .forAccountId(invalidAccountId)
        .forPageSize("10")
        .forDirection("next")
        .forVrn("ABC123")
        .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
        .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
        .then()
        .responseIsReturnedWithStatusCode(404);
  }

  @Test
  public void shouldReturnHttpServiceUnavailableWhenAccountServiceDown() {
    mockAccountServiceCursorCallWithError(ACCOUNT_ID, "ABC123", 503);
    givenAssertion()
        .forAccountId(ACCOUNT_ID)
        .forPageSize("10")
        .forDirection("next")
        .forVrn("ABC123")
        .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
        .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
        .then()
        .responseIsReturnedWithStatusCode(503);
  }

  public RetrieveChargeableAccountVehiclesJourneyAssertion givenAssertion() {
    return new RetrieveChargeableAccountVehiclesJourneyAssertion();
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  private List<String> getAccountVehicleVrnsFromFile(String fileName)
      throws JsonProcessingException {
    ChargeableVehiclesResponseDto response = new ObjectMapper()
        .readValue(readJson(fileName), ChargeableVehiclesResponseDto.class);
    return response.getVehicles().stream()
        .sorted(Comparator.comparing(VehicleWithCharges::getVrn))
        .map(VehicleWithCharges::getVrn)
        .collect(Collectors.toList());
  }

  private List<String> mockChargeableVehiclesWithCursor() throws JsonProcessingException {
    mockAccountServiceCursorCall(ACCOUNT_ID, "ABC123", NEXT_CURSOR_RESPONSE);
    List<String> vrns = getAccountVehicleVrnsFromFile(NEXT_CURSOR_RESPONSE);
    return vrns;
  }

  private void mockEmptyChargeableVehiclesWithCursor() {
    mockAccountServiceCursorCall(ACCOUNT_ID, "ABC123", EMPTY_CURSOR_RESPONSE);
  }

  private List<String> mockChargeableVehiclesWithoutCursor() throws JsonProcessingException {
    mockAccountServiceCursorCallWithoutCursorParameter(ACCOUNT_ID, NEXT_CURSOR_RESPONSE);
    List<String> vrns = getAccountVehicleVrnsFromFile(NEXT_CURSOR_RESPONSE);
    return vrns;
  }
}
