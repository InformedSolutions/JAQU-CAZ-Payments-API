package uk.gov.caz.psr;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

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
@Sql(scripts = "classpath:data/sql/charge-settlement/payment-info/test-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class PaymentDetailsTestIT {

  public static final String CORRELATION_ID = "79b7a48f-27c7-4947-bd1c-670f981843ef";

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
    String paymentId = "eb3f1a6a-102c-11ea-be9e-2b1c2964eba8";

    RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .pathParam("payment_id", paymentId)

        .when()
        .get(PaymentsController.GET_PAYMENT_DETAILS)

        .then()
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .statusCode(HttpStatus.OK.value())
        .body("centralPaymentReference", notNullValue())
        .body("paymentProviderId", equalTo("ext-payment-id-3"))
        .body("paymentDate", equalTo("2019-11-23"))
        .body("totalPaid", equalTo(1.4f))
        .body("telephonePayment", equalTo(false))
        .body("lineItems.size()", equalTo(2))
        .body("lineItems[0].chargePaid.", equalTo(0.7f))
        .body("lineItems[0].travelDate.", equalTo("2019-11-01"))
        .body("lineItems[0].vrn.", equalTo("AB11CDE"))
        .body("lineItems[1].chargePaid.", equalTo(0.7f))
        .body("lineItems[1].travelDate.", equalTo("2019-11-02"))
        .body("lineItems[1].vrn.", equalTo("AB11CDE"));
  }

  @Test
  void shouldReturnNotFound() {
    String paymentId = "eb3f1a6a-102c-11ea-be9e-2b1c2964eba7";

    RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .pathParam("payment_id", paymentId)

        .when()
        .get(PaymentsController.GET_PAYMENT_DETAILS)

        .then()
        .header(Constants.X_CORRELATION_ID_HEADER, CORRELATION_ID)
        .statusCode(HttpStatus.NOT_FOUND.value());
  }
}
