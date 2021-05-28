package uk.gov.caz.psr;

import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.journeys.RetrieveSuccessfulPaymentsJourneyAssertion;

@Sql(scripts = "classpath:data/sql/add-caz-entrant-payments.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@FullyRunningServerIntegrationTest
public class RetrieveSuccessfulPaymentsTestIT extends ExternalCallsIT {

  private static final String ANY_ACCOUNT_ID = UUID.randomUUID().toString();
  private static final String OWNER_USER_UD = "ab3e9f4b-4076-4154-b6dd-97c5d4800b47";
  private static final String NON_EXISTING_USER_ID = "cd5b8fbc-f106-4726-96ea-ef76d1136cd8";

  private static final String ANY_PAGE_NUMBER = "0";
  private static final String ANY_PAGE_SIZE = "10";

  @LocalServerPort
  int randomServerPort;

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
  }

  @Test
  public void shouldReturn200OkResponseWhenValidRequestForAllUsersAssociatedWithAccount() {
    mockAccountServiceGetAllUsersCall(ANY_ACCOUNT_ID, 200);
    mockVccsCleanAirZonesCall();

    givenSuccessfulPaymentsRetrieval()
        .forAccountId(ANY_ACCOUNT_ID)
        .forPageNumber(ANY_PAGE_NUMBER)
        .forPageSize(ANY_PAGE_SIZE)
        .whenRequestIsMade()
        .then()
        .responseIsReturnedWithHttpOkStatusCode()
        .responseIncludeDataOfAllUsers();
  }

  @Test
  public void shouldReturn200OkResponseWhenValidRequestForSingleUser() {
    mockAccountServiceGetAllUsersCall(ANY_ACCOUNT_ID, 200);
    mockVccsCleanAirZonesCall();

    givenSuccessfulPaymentsRetrieval()
        .forAccountId(ANY_ACCOUNT_ID)
        .forAccountUserId(OWNER_USER_UD)
        .forPageNumber(ANY_PAGE_NUMBER)
        .forPageSize(ANY_PAGE_SIZE)
        .whenRequestIsMade()
        .then()
        .responseIsReturnedWithHttpOkStatusCode()
        .responseIncludeDataOfASingleUser()
        .responseIncludeRequiredFlags();
  }

  @Test
  public void shouldReturn200OkEmptyResponseWhenRequestingNonAssociatedUser() {
    mockAccountServiceGetAllUsersCall(ANY_ACCOUNT_ID, 200);
    mockVccsCleanAirZonesCall();

    givenSuccessfulPaymentsRetrieval()
        .forAccountId(ANY_ACCOUNT_ID)
        .forAccountUserId(NON_EXISTING_USER_ID)
        .forPageNumber(ANY_PAGE_NUMBER)
        .forPageSize(ANY_PAGE_SIZE)
        .whenRequestIsMade()
        .then()
        .responseIsReturnedWithHttpOkStatusCode()
        .responseHadNoData();
  }

  private RetrieveSuccessfulPaymentsJourneyAssertion givenSuccessfulPaymentsRetrieval() {
    return new RetrieveSuccessfulPaymentsJourneyAssertion();
  }
}
