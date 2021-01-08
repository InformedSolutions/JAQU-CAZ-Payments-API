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
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.journeys.RetrieveChargeableAccountVehiclesJourneyAssertion;

@FullyRunningServerIntegrationTest
public class RetrieveChargeableAccountVehiclesIT extends ExternalCallsIT {

  private static final String CLEAN_AIR_ZONE_ID = "5cd7441d-766f-48ff-b8ad-1809586fea37";
  private static final String ACCOUNT_ID = UUID.randomUUID().toString();
  private static final String ACCOUNT_VEHICLES_RESPONSE = "account-vehicles-response.json";
  private static final String EMPTY_ACCOUNT_VEHICLES_RESPONSE = "account-vehicles-empty-response.json";

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
  public void shouldReturnHttpOkWhenValidRequestWithFirstPage()
      throws JsonProcessingException {

    List<String> vrns = mockChargeableVehiclesWithPage();

    givenAssertion()
        .forAccountId(ACCOUNT_ID)
        .forPageSize("10")
        .forPage("1")
        .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
        .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
        .then()
        .responseIsReturnedWithHttpOkStatusCode()
        .responseContainsExpectedData(vrns, 20L, 2);
  }

  @Test
  public void shouldReturnHttpOkAndEmptyArrayWhenNoVrnsCanBeFound() {
    mockEmptyChargeableVehicles();

    givenAssertion()
        .forAccountId(ACCOUNT_ID)
        .forPageSize("10")
        .forPage("1")
        .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
        .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
        .then()
        .responseIsReturnedWithHttpOkStatusCode()
        .responseContainsExpectedData(Collections.emptyList(), 0, 0);
  }

  @ParameterizedTest
  @CsvSource({",,", "1,,10", "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,,",
      "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,,-1", "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,,0",
      "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,-1,0", "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,-1,10",
      "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,-1,a", "4dc6ea23-77d3-4bfe-8180-7662c33f88ad,b,10"})
  public void shouldReturnHttpBadRequestWhenInvalidQueryStringsSupplied(String cleanAirZoneId,
      String pageSize, String pageNumber) {
    mockAccountServiceVehiclesCallWithoutParameter(ACCOUNT_ID, ACCOUNT_VEHICLES_RESPONSE);
    givenAssertion()
        .forAccountId(ACCOUNT_ID)
        .forPageSize(pageSize)
        .forPage(pageNumber)
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
    mockAccountServiceVehiclesCallWithError(invalidAccountId, "1", 404);
    givenAssertion()
        .forAccountId(invalidAccountId)
        .forPageSize("10")
        .forPage("1")
        .forCleanAirZoneId(CLEAN_AIR_ZONE_ID)
        .whenRequestIsMadeToRetrieveChargeableAccountVehicles()
        .then()
        .responseIsReturnedWithStatusCode(404);
  }

  @Test
  public void shouldReturnHttpServiceUnavailableWhenAccountServiceDown() {
    mockAccountServiceVehiclesCallWithError(ACCOUNT_ID, "1", 503);
    givenAssertion()
        .forAccountId(ACCOUNT_ID)
        .forPageSize("10")
        .forPage("1")
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
    VehiclesResponseDto response = new ObjectMapper()
        .readValue(readJson(fileName), VehiclesResponseDto.class);
    return response.getVehicles().stream()
        .sorted(Comparator.comparing(VehicleWithCharges::getVrn))
        .map(VehicleWithCharges::getVrn)
        .collect(Collectors.toList());
  }

  private List<String> mockChargeableVehiclesWithPage() throws JsonProcessingException {
    mockAccountServiceVehiclesCall(ACCOUNT_ID, "1", ACCOUNT_VEHICLES_RESPONSE);
    List<String> vrns = getAccountVehicleVrnsFromFile(ACCOUNT_VEHICLES_RESPONSE);
    return vrns;
  }

  private void mockEmptyChargeableVehicles() {
    mockAccountServiceVehiclesCall(ACCOUNT_ID, "1", EMPTY_ACCOUNT_VEHICLES_RESPONSE);
  }
}
