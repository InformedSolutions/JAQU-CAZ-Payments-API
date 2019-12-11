package uk.gov.caz.psr;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.controller.ChargeSettlementController;
import uk.gov.caz.psr.dto.Headers;
import uk.gov.caz.psr.dto.PaymentStatusResponse;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.repository.PaymentStatusRepository;
import uk.gov.caz.psr.util.TestObjectFactory.PaymentStatusFactory;

@Sql(scripts = "classpath:data/sql/add-payments-for-payment-status.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@IntegrationTest
@AutoConfigureMockMvc
public class GetChargeSettlementPaymentStatusTestIT {

  private static final String VALID_VRN = "ND84VSX";
  private static final String VALID_DATE_STRING = "2019-11-01";

  private static final String VALID_DATE_WITH_STATUSES_WHICH_IS_DIFFERENT_THAN_PAID = "2019-11-02";
  private static final String VALID_DATE_WITH_DIFFERENT_STATUSES = "2019-11-03";
  private static final String VALID_DUPLICATED_DATE_STRING = "2019-11-04";
  private static final String VALID_CAZ_ID = "b8e53786-c5ca-426a-a701-b14ee74857d4";
  private static final String VALID_CORRELATION_HEADER = "79b7a48f-27c7-4947-bd1c-670f981843ef";

  private static final String VALID_EXTERNAL_ID = "54321test";
  private static final String VALID_CASE_REFERENCE = "case-reference123";
  private static final String PAYMENT_STATUS_GET_PATH = ChargeSettlementController.BASE_PATH +
      ChargeSettlementController.PAYMENT_STATUS_PATH;

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private PaymentStatusRepository paymentStatusRepository;

  @Test
  public void shouldReturn200WithNotPaidStatusWhenDoesNotExistInDatabase() throws Exception {
    String nonExistingVrn = "CAS222";

    mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(Constants.X_CORRELATION_ID_HEADER, VALID_CORRELATION_HEADER)
        .header(Headers.TIMESTAMP, LocalDateTime.now())
        .header(Headers.X_API_KEY, VALID_CAZ_ID)
        .param("vrn", nonExistingVrn)
        .param("dateOfCazEntry", VALID_DATE_STRING))
        .andExpect(status().isOk())
        .andExpect(content().json(getNotPaidResponse()));

  }

  @Test
  public void shouldReturn500WhenThereAreTwoPaidPaymentStatusesWithSameVrnAndDateOfEntrance()
      throws Exception {
    mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(Constants.X_CORRELATION_ID_HEADER, VALID_CORRELATION_HEADER)
        .header(Headers.TIMESTAMP, LocalDateTime.now())
        .header(Headers.X_API_KEY, VALID_CAZ_ID)
        .param("vrn", VALID_VRN)
        .param("dateOfCazEntry", VALID_DUPLICATED_DATE_STRING))
        .andExpect(status().is5xxServerError())
        .andExpect(
            jsonPath("$.errors[0].detail").value("More than one paid VehicleEntrantPayment found"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "ND84VSX", // existing
      "Nd84vSX", "nD84vsX", // with changed capitalisation
      "ND 84 VSX", "  ND84V S X ", "N D8   4VSX", // with whitespaces
      "N D8  4v SX " // with whitespaces and changed capitalisation
  })
  public void shouldReturn200AndPaidPaymentStatusWhenThereAreManyRecordsAndOneOfThemIsPaid(
      String vrn)
      throws Exception {
    mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(Constants.X_CORRELATION_ID_HEADER, VALID_CORRELATION_HEADER)
        .header(Headers.TIMESTAMP, LocalDateTime.now())
        .header(Headers.X_API_KEY, VALID_CAZ_ID)
        .param("vrn", vrn)
        .param("dateOfCazEntry", VALID_DATE_WITH_DIFFERENT_STATUSES))
        .andExpect(status().isOk())
        .andExpect(content().json(
            getResponseWith(InternalPaymentStatus.PAID, VALID_CASE_REFERENCE, VALID_EXTERNAL_ID)));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "ND84VSX", // existing
      "Nd84vSX", "nD84vsX", // with changed capitalisation
      "ND 84 VSX", "  ND84V S X ", "N D8   4VSX", // with whitespaces
      "N D8  4v SX " // with whitespaces and changed capitalisation
  })
  public void shouldReturn200AndTheExistingPaymentWhenThereAreNoPaid(String vrn) throws Exception {
    mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(Constants.X_CORRELATION_ID_HEADER, VALID_CORRELATION_HEADER)
        .header(Headers.TIMESTAMP, LocalDateTime.now())
        .header(Headers.X_API_KEY, VALID_CAZ_ID)
        .param("vrn", vrn)
        .param("dateOfCazEntry", VALID_DATE_WITH_STATUSES_WHICH_IS_DIFFERENT_THAN_PAID))
        .andExpect(status().isOk())
        .andExpect(content().json(
            getResponseWith(InternalPaymentStatus.REFUNDED, VALID_CASE_REFERENCE,
                VALID_EXTERNAL_ID)));
  }

  private String getNotPaidResponse() {
    PaymentStatusResponse paymentStatusResponse = PaymentStatusResponse
        .from(PaymentStatus.getEmptyPaymentStatusResponse());

    return toJsonString(paymentStatusResponse);
  }

  private String getResponseWith(InternalPaymentStatus internalPaymentStatus, String caseReference,
      String externalId) {
    PaymentStatusResponse paymentStatusResponse = PaymentStatusResponse
        .from(PaymentStatusFactory.with(
            internalPaymentStatus,
            caseReference,
            externalId
        ));

    return toJsonString(paymentStatusResponse);
  }

  @SneakyThrows
  private String toJsonString(Object response) {
    return objectMapper.writeValueAsString(response);
  }
}
