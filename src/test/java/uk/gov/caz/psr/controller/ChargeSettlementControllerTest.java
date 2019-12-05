package uk.gov.caz.psr.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.psr.controller.ChargeSettlementController.BASE_PATH;
import static uk.gov.caz.psr.controller.ChargeSettlementController.PAYMENT_INFO_PATH;
import static uk.gov.caz.psr.controller.ChargeSettlementController.PAYMENT_STATUS_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.configuration.MessageBundleConfiguration;
import uk.gov.caz.psr.dto.ChargeSettlementPaymentStatus;
import uk.gov.caz.psr.dto.PaymentStatusUpdateDetails;
import uk.gov.caz.psr.dto.PaymentStatusUpdateRequest;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.service.ChargeSettlementPaymentInfoService;
import uk.gov.caz.psr.service.ChargeSettlementService;
import uk.gov.caz.psr.service.PaymentStatusUpdateService;
import uk.gov.caz.psr.util.TestObjectFactory.PaymentStatusFactory;
import uk.gov.caz.psr.util.TestObjectFactory.PaymentStatusUpdateDetailsFactory;
import uk.gov.caz.psr.util.VehicleEntrantPaymentInfoConverter;

@ContextConfiguration(classes = {GlobalExceptionHandlerConfiguration.class, Configuration.class,
    ChargeSettlementController.class, ExceptionController.class, MessageBundleConfiguration.class})
@WebMvcTest
class ChargeSettlementControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private PaymentStatusUpdateService paymentStatusUpdateService;
  @MockBean
  private ChargeSettlementService chargeSettlementService;
  @MockBean
  private ChargeSettlementPaymentInfoService chargeSettlementPaymentInfoService;
  @MockBean
  private VehicleEntrantPaymentInfoConverter vehicleEntrantPaymentInfoConverter;

  private static final String ANY_VALID_VRN = "DL76MWX";
  private static final String ANY_VALID_DATE_STRING = LocalDate.now().toString();
  private static final String ANY_CORRELATION_ID = "1f7c6a8c-15a3-11ea-b483-afe9911b08f0";
  private static final String ANY_API_KEY = "e6d892be-15a2-11ea-b483-a3ea711c89e8";
  private static final String ANY_PAYMENT_ID = "payment-id";
  private static final String PAYMENT_INFO_GET_PATH = BASE_PATH + "/" + PAYMENT_INFO_PATH;
  private static final String PAYMENT_STATUS_GET_PATH = BASE_PATH + "/" + PAYMENT_STATUS_PATH;
  private static final String PAYMENT_STATUS_PUT_PATH = BASE_PATH + PAYMENT_STATUS_PATH;

  @Nested
  class GetPaymentInfo {

    @Test
    public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
      String payload = "";

      mockMvc.perform(get(PAYMENT_INFO_GET_PATH)
          .content(payload)
          .accept(MediaType.APPLICATION_JSON)
          .header(Headers.X_API_KEY, ANY_API_KEY))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message")
              .value("Missing request header 'X-Correlation-ID'"));
    }

    @Test
    public void shouldReturn400StatusCodeWhenAllParametersAreMissing() throws Exception {
      mockMvc.perform(get(PAYMENT_INFO_GET_PATH)
          .accept(MediaType.APPLICATION_JSON)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .header(Headers.X_API_KEY, ANY_API_KEY))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].title").value("Parameter validation error"))
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrn").doesNotExist())
          .andExpect(jsonPath("$.errors[0].detail").value("Request must contain at least one parameter"));
    }

    @Nested
    class Dates {

      @Nested
      class ToDatePaidFor {
        @ParameterizedTest
        @ValueSource(strings = {"01.10.2019", "28/04/2007", "not-a-valid-date"})
        public void shouldReturn400StatusCodeUponInvalidFormat(String invalidToDatePaidFor) throws Exception {
          invalidDateFormatTest(invalidToDatePaidFor, "toDatePaidFor");
        }
      }

      @Nested
      class FromDatePaidFor {
        @ParameterizedTest
        @ValueSource(strings = {"07.09.2019", "11/03/2006", "not-a-valid-date"})
        public void shouldReturn400StatusCodeUponInvalidFormat(String invalidFromDatePaidFor) throws Exception {
          invalidDateFormatTest(invalidFromDatePaidFor, "fromDatePaidFor");
        }
      }

      private void invalidDateFormatTest(String invalidDate, String argumentName)
          throws Exception {
        mockMvc.perform(get(PAYMENT_INFO_GET_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .param(argumentName, invalidDate)
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .header(Headers.X_API_KEY, ANY_API_KEY))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].title").value("Parameter validation error"))
            .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.errors[0].vrn").doesNotExist())
            .andExpect(jsonPath("$.errors[0].detail")
                .value("Invalid date format of \"" + argumentName + "\""));
      }
    }

    @Nested
    class Vrn {

      @ParameterizedTest
      @ValueSource(strings = {"", "TOO_LONG_VRN_1234567890"})
      public void shouldReturn400StatusCodeUponVrnWithInvalidLength(String invalidVrn)
          throws Exception {
        mockMvc.perform(get(PAYMENT_INFO_GET_PATH)
            .param("vrn", invalidVrn)
            .accept(MediaType.APPLICATION_JSON)
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .header(Headers.X_API_KEY, ANY_API_KEY))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].title").value("Parameter validation error"))
            .andExpect(jsonPath("$.errors[0].vrn").doesNotExist())
            .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.errors[0].detail").value("\"vrn\" size must be between 1 and 15"));
      }
    }

    @Nested
    class PaymentProviderId {

      @ParameterizedTest
      @ValueSource(strings = {
          "",
          "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
              + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
              + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
              + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      })
      public void shouldReturn400StatusCodeUponPaymentIdWithInvalidLength(String paymentProviderId)
          throws Exception {
        mockMvc.perform(get(PAYMENT_INFO_GET_PATH)
            .param("paymentProviderId", paymentProviderId)
            .accept(MediaType.APPLICATION_JSON)
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .header(Headers.X_API_KEY, ANY_API_KEY))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].title").value("Parameter validation error"))
            .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.errors[0].vrn").doesNotExist())
            .andExpect(jsonPath("$.errors[0].detail").value("\"paymentProviderId\" size must be between 1 and 255"));
      }
    }

    @Test
    public void shouldReturn200StatusCodeWhenParametersAreValid() throws Exception {
      mockMvc.perform(get(PAYMENT_INFO_GET_PATH)
          .param("paymentProviderId", ANY_PAYMENT_ID)
          .param("vrn", ANY_VALID_VRN)
          .param("toDatePaidFor", ANY_VALID_DATE_STRING)
          .param("fromDatePaidFor", ANY_VALID_DATE_STRING)
          .accept(MediaType.APPLICATION_JSON)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .header(Headers.X_API_KEY, ANY_API_KEY))
          .andExpect(status().isOk());
    }
  }

  @Nested
  class GetPaymentStatus {

    @Test
    public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
      String payload = "";

      mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message")
              .value("Missing request header 'X-Correlation-ID'"));
    }

    @Test
    public void shouldReturn400StatusCodeWhenVrnIsMissing() throws Exception {
      mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .param("dateOfCazEntry", ANY_VALID_DATE_STRING))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn400StatusWhenDateOfCazEntryIsMissing() throws Exception {
      mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .param("vrn", ANY_VALID_VRN))
          .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "UNSOPHISTICATION"})
    public void shouldReturn400StatusCodeWhenVrnIsInvalid(String vrn) throws Exception {
      mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .param("vrn", vrn)
          .param("dateOfCazEntry", ANY_VALID_DATE_STRING))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn200StatusCodeWhenRequestIsValid() throws Exception {
      PaymentStatus paymentStatusStub = PaymentStatusFactory
          .anyWithStatus(InternalPaymentStatus.PAID);

      given(chargeSettlementService.findChargeSettlement(any(), any(), any()))
          .willReturn(paymentStatusStub);

      mockMvc.perform(get(PAYMENT_STATUS_GET_PATH)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .header(Headers.X_API_KEY, UUID.randomUUID())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
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
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header(Headers.X_API_KEY, UUID.randomUUID()))
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
          .accept(MediaType.APPLICATION_JSON)
          .header(Headers.X_API_KEY, UUID.randomUUID()))
          .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void emptyVrnShouldResultIn400() throws Exception {
      String payload = requestWithVrn("");

      mockMvc.perform(put(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .header(Headers.X_API_KEY, UUID.randomUUID()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrn").value(""))
          .andExpect(jsonPath("$.errors[*].title").value(hasItem("Parameter validation error")))
          .andExpect(jsonPath("$.errors[*].detail").value(hasItem("\"vrn\" is mandatory and cannot be blank")));
    }

    @Test
    public void nullVrnShouldResultIn400() throws Exception {
      String payload = requestWithVrn(null);

      mockMvc.perform(put(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .header(Headers.X_API_KEY, UUID.randomUUID()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrn").value(IsNull.nullValue()))
          .andExpect(jsonPath("$.errors[*].title").value(hasItem("Parameter validation error")))
          .andExpect(jsonPath("$.errors[*].detail").value(hasItem("\"vrn\" is mandatory and cannot be blank")));
    }

    @Test
    public void invalidVrnShouldResultIn400() throws Exception {
      String payload = requestWithVrn("TOO_LONG_VRN_1234567890");

      mockMvc.perform(put(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .header(Headers.X_API_KEY, UUID.randomUUID()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].title").value("Parameter validation error"))
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrn").value("TOO_LONG_VRN_1234567890"))
          .andExpect(jsonPath("$.errors[0].detail").value("\"vrn\" size must be between 1 and 15"));
    }

    @Test
    public void emptyStatusUpdatesShouldResultIn400() throws Exception {
      String payload = requestWithStatusUpdates(null);

      mockMvc.perform(put(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .header(Headers.X_API_KEY, UUID.randomUUID()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].title").value("Parameter validation error"))
          .andExpect(jsonPath("$.errors[0].vrn").isNotEmpty())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].detail").value("\"statusUpdates\" is mandatory and cannot be empty"));
    }

    @Test
    public void blankCaseReferenceShouldResultIn400() throws Exception {
      String payload = requestWithStatusUpdates(
          Collections.singletonList(PaymentStatusUpdateDetailsFactory.anyInvalid()));

      mockMvc.perform(put(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .header(Headers.X_API_KEY, UUID.randomUUID()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].title").value("Parameter validation error"))
          .andExpect(jsonPath("$.errors[0].vrn").isNotEmpty())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].detail")
              .value("\"statusUpdates[0].caseReference\" is mandatory and cannot be blank"));
    }

    @Test
    public void tooLongCaseReferenceShouldResultIn400() throws Exception {
      String payload = requestWithStatusUpdates(
          Collections.singletonList(
              PaymentStatusUpdateDetailsFactory.withCaseReference(Strings.repeat("b", 16))));

      mockMvc.perform(put(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .header(Headers.X_API_KEY, UUID.randomUUID()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].title").value("Parameter validation error"))
          .andExpect(jsonPath("$.errors[0].vrn").isNotEmpty())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].detail")
              .value("\"statusUpdates[0].caseReference\" size must be between 1 and 15"));
    }

    @Test
    public void invalidPaymentIdShouldResultIn400() throws Exception {
      PaymentStatusUpdateRequest request = baseRequestBuilder()
          .statusUpdates(Collections.singletonList(
              PaymentStatusUpdateDetailsFactory.withPaymentId(
                  Strings.repeat("a", 256))))
          .build();
      String payload = toJsonString(request);

      mockMvc.perform(put(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .header(Headers.X_API_KEY, UUID.randomUUID()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].title").value("Parameter validation error"))
          .andExpect(jsonPath("$.errors[0].vrn").value(request.getVrn()))
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].detail")
              .value("\"statusUpdates[0].paymentProviderId\" size must be between 1 and 255"));
    }

    @Test
    public void validRequestShouldResultIn200() throws Exception {
      String payload = toJsonString(baseRequestBuilder().build());

      mockMvc.perform(put(PAYMENT_STATUS_PUT_PATH)
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .header(Headers.X_API_KEY, UUID.randomUUID()))
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