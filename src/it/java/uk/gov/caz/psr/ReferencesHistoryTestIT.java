package uk.gov.caz.psr;

import static org.hamcrest.CoreMatchers.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.controller.PaymentsController;

@FullyRunningServerIntegrationTest
@Sql(scripts = "classpath:data/sql/references-history-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class ReferencesHistoryTestIT extends ExternalCallsIT{

  private static final String CORRELATION_ID = "79b7a48f-27c7-4947-bd1c-670f981843ef";

  @Autowired
  ObjectMapper objectMapper;

  @LocalServerPort
  int randomServerPort;

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = "/v1/payments";
  }

  @Test
  void shouldReturnSuccessResponse() {
    mockVccsCleanAirZonesCall();
    Long paymentReference = 2000L;

    RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .pathParam("paymentReference", paymentReference)

        .when()
        .get(PaymentsController.GET_REFERENCES_HISTORY)

        .then()
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .statusCode(HttpStatus.OK.value())
        .body("paymentReference", equalTo(2000))
        .body("paymentProviderId", equalTo("ext-payment-id-3"))
        .body("paymentTimestamp", equalTo("2020-05-01T11:43:41"))
        .body("totalPaid", equalTo(140))
        .body("telephonePayment", equalTo(false))
        .body("operatorId", equalTo("e9a92b87-057a-4578-afb1-61d8b9af1569"))
        .body("cazName", equalTo("Birmingham"))
        .body("paymentProviderStatus", equalTo("SUCCESS"))
        .body("lineItems.size()", equalTo(2))
        .body("lineItems[0].chargePaid.", equalTo(70))
        .body("lineItems[0].travelDate.", equalTo("2019-11-01"))
        .body("lineItems[0].vrn.", equalTo("AB11CDE"))
        .body("lineItems[1].chargePaid.", equalTo(70))
        .body("lineItems[1].travelDate.", equalTo("2019-11-02"))
        .body("lineItems[1].vrn.", equalTo("AB11CDE"));
  }

  @Test
  void shouldReturnNotFound() {
    Long paymentReference = 1200L;

    RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .pathParam("paymentReference", paymentReference)

        .when()
        .get(PaymentsController.GET_REFERENCES_HISTORY)

        .then()
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .statusCode(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void shouldReturn500WhenThereIsNotUniqueCazInEntrantPaymentsForTheSamePayment() {
    mockVccsCleanAirZonesCall();
    Long paymentReference = 2500L;

    RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .pathParam("paymentReference", paymentReference)

        .when()
        .get(PaymentsController.GET_REFERENCES_HISTORY)

        .then()
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
  }
}