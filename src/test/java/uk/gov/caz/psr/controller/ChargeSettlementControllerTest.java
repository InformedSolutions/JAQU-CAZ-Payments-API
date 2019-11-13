package uk.gov.caz.psr.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.psr.controller.ChargeSettlementController.BASE_PATH;
import static uk.gov.caz.psr.controller.ChargeSettlementController.PAYMENT_STATUS_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.ChargeSettlementPaymentStatus;
import uk.gov.caz.psr.dto.PaymentStatusUpdateDetails;
import uk.gov.caz.psr.dto.PaymentStatusUpdateRequest;
import uk.gov.caz.psr.util.TestObjectFactory.PaymentStatusUpdateDetailsFactory;

@ContextConfiguration(classes = {GlobalExceptionHandlerConfiguration.class, Configuration.class,
    ChargeSettlementController.class})
@WebMvcTest
class ChargeSettlementControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String ANY_VALID_VRN = "DL76MWX";
  private static final String ANY_VALID_DATE_STRING = LocalDate.now().toString();
  private static final String ANY_CORRELATION_ID = UUID.randomUUID().toString();
  private static final String PAYMENT_STATUS_GET_PATH = BASE_PATH + "/" + PAYMENT_STATUS_PATH;
  private static final String PAYMENT_STATUS_PUT_PATH = BASE_PATH + PAYMENT_STATUS_PATH;

  @Nested
  class PaymentStatus {

    @Test
    public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
      String payload = "";

      mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message")
              .value("Missing request header 'X-Correlation-ID'"));
    }

    @Test
    public void shouldReturn400StatusCodeWhenVrnIsMissing() throws Exception {
      mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8)
          .param("dateOfCazEntry", ANY_VALID_DATE_STRING))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn400StatusWhenDateOfCazEntryIsMissing() throws Exception {
      mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON_UTF8)
          .accept(MediaType.APPLICATION_JSON_UTF8)
          .param("vrn", ANY_VALID_VRN))
          .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "UNSOPHISTICATION"})
    public void shouldReturn400StatusCodeWhenVrnIsInvalid(String vrn) throws Exception {
      mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8)
          .param("vrn", vrn)
          .param("dateOfCazEntry", ANY_VALID_DATE_STRING))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn200StatusCodeWhenRequestIsValid() throws Exception {
      mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8)
          .param("vrn", ANY_VALID_VRN)
          .param("dateOfCazEntry", ANY_VALID_DATE_STRING))
          .andExpect(status().isOk());
    }
  }

  @Nested
  class PaymentStatusUpdate {

    @Test
    public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
      String payload = "";

      mockMvc.perform(put(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message")
              .value("Missing request header 'X-Correlation-ID'"));
    }

    @Test
    public void shouldReturn405StatusCodeForPostRequest() throws Exception {
      String payload = "";

      mockMvc.perform(post(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void emptyVrnShouldResultIn400() throws Exception {
      String payload = requestWithVrn("");

      mockMvc.perform(put(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void emptyStatusUpdatesShouldResultIn400() throws Exception {
      String payload = requestWithStatusUpdates(null);

      mockMvc.perform(put(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void validRequestShouldResultIn200() throws Exception {
      String payload = toJsonString(baseRequestBuilder().build());

      mockMvc.perform(put(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isOk());
    }

    private PaymentStatusUpdateRequest.PaymentStatusUpdateRequestBuilder baseRequestBuilder() {
      return PaymentStatusUpdateRequest.builder()
          .vrn(ANY_VALID_VRN)
          .statusUpdates(buildPaymentStatusUpdateDetails());
    }

    private List<PaymentStatusUpdateDetails> buildPaymentStatusUpdateDetails() {
      return Arrays.asList(
          PaymentStatusUpdateDetailsFactory.anyWithStatus(ChargeSettlementPaymentStatus.CHARGEBACK),
          PaymentStatusUpdateDetailsFactory.anyWithStatus(ChargeSettlementPaymentStatus.REFUNDED));
    }

    private String requestWithVrn(String vrn) {
      PaymentStatusUpdateRequest request = baseRequestBuilder().vrn(vrn).build();
      return toJsonString(request);
    }

    private String requestWithStatusUpdates(List<PaymentStatusUpdateDetails> statusUpdates) {
      PaymentStatusUpdateRequest request = baseRequestBuilder().statusUpdates(statusUpdates)
          .build();
      return toJsonString(request);
    }
  }

  @SneakyThrows
  private String toJsonString(PaymentStatusUpdateRequest requestParams) {
    return objectMapper.writeValueAsString(requestParams);
  }
}