package uk.gov.caz.psr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.psr.controller.PaymentsController.BASE_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.PaidPaymentsRequest;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.GetPaidEntrantPaymentsService;
import uk.gov.caz.psr.service.InitiatePaymentService;
import uk.gov.caz.psr.service.ReconcilePaymentStatusService;
import uk.gov.caz.psr.util.TestObjectFactory;
import uk.gov.caz.psr.util.TestObjectFactory.EntrantPayments;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ContextConfiguration(classes = {ExceptionController.class, Configuration.class,
    PaymentsController.class})
@WebMvcTest
class PaymentsControllerTest {

  @MockBean
  private InitiatePaymentService initiatePaymentService;

  @MockBean
  private ReconcilePaymentStatusService reconcilePaymentStatusService;

  @MockBean
  private GetPaidEntrantPaymentsService getPaidEntrantPaymentsService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  public void resetMocks() {
    Mockito.reset(initiatePaymentService);
    Mockito.reset(getPaidEntrantPaymentsService);
  }

  private static final List<LocalDate> days =
      Arrays.asList(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3));

  private static final String ANY_CORRELATION_ID = UUID.randomUUID().toString();

  private static final String GET_PAID_PATH = PaymentsController.BASE_PATH + "/"
      + PaymentsController.GET_PAID_VEHICLE_ENTRANTS;

  @Nested
  class InitiatePayment {

    @Test
    public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
      String payload = "";

      mockMvc
          .perform(post(BASE_PATH).content(payload).contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Missing request header 'X-Correlation-ID'"));
      verify(initiatePaymentService, never()).createPayment(any());
    }

    @Test
    public void emptyDaysShouldResultIn400() throws Exception {
      String payload = paymentRequestWithEmptyDays();

      performRequestWithContent(payload).andExpect(status().isBadRequest());
      verify(initiatePaymentService, never()).createPayment(any());
    }

    @Test
    public void invalidVrnShouldResultIn400() throws Exception {
      String payload = paymentRequestWithVrn("1234567890123456");

      performRequestWithContent(payload).andExpect(status().isBadRequest());
      verify(initiatePaymentService, never()).createPayment(any());
    }

    @Test
    public void invalidAmountShouldResultIn400() throws Exception {
      String payload = paymentRequestWithAmount(-1250);

      performRequestWithContent(payload).andExpect(status().isBadRequest());
      verify(initiatePaymentService, never()).createPayment(any());
    }

    @Test
    public void invalidReturnUrlShouldResultIn400() throws Exception {
      String payload = paymentRequestWithEmptyReturnUrl();

      performRequestWithContent(payload).andExpect(status().isBadRequest());
      verify(initiatePaymentService, never()).createPayment(any());
    }

    @Test
    @Disabled // TODO enable once the whole flow with passing tariffCode from UI is completed
    public void invalidTariffCodeShouldResultIn400() throws Exception {
      String payload = paymentRequestWithEmptyTariffCode();

      performRequestWithContent(payload).andExpect(status().isBadRequest());
      verify(initiatePaymentService, never()).createPayment(any());
    }

    @Test
    public void shouldReturnValidResponse() throws Exception {
      InitiatePaymentRequest requestParams = baseRequestBuilder().build();
      String payload = toJsonString(requestParams);
      Payment successfullyCreatedPayment = Payments.existing();
      given(initiatePaymentService.createPayment(requestParams))
          .willReturn(successfullyCreatedPayment);

      performRequestWithContent(payload)
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.paymentId").value(successfullyCreatedPayment.getId().toString()))
          .andExpect(jsonPath("$.nextUrl").value(successfullyCreatedPayment.getNextUrl()));
      verify(initiatePaymentService).createPayment(requestParams);
    }

    private ResultActions performRequestWithContent(String payload) throws Exception {
      return mockMvc.perform(post(BASE_PATH)
          .content(payload).contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
    }

    private String paymentRequestWithEmptyDays() {
      List<LocalDate> emptyDays = Collections.emptyList();
      InitiatePaymentRequest requestParams = baseRequestBuilder().days(emptyDays).build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithVrn(String vrn) {
      InitiatePaymentRequest requestParams = baseRequestBuilder().vrn(vrn).build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithAmount(Integer amount) {
      InitiatePaymentRequest requestParams = baseRequestBuilder().amount(amount).build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithEmptyReturnUrl() {
      InitiatePaymentRequest requestParams = baseRequestBuilder().returnUrl("").build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithEmptyTariffCode() {
      InitiatePaymentRequest requestParams = baseRequestBuilder().tariffCode("").build();
      return toJsonString(requestParams);
    }

    private InitiatePaymentRequest.InitiatePaymentRequestBuilder baseRequestBuilder() {
      return InitiatePaymentRequest.builder().cleanAirZoneId(UUID.randomUUID()).days(days)
          .vrn("TEST123").amount(1050).returnUrl("https://example.return.url")
          .tariffCode("BCC01-private_car");
    }
  }

  @Nested
  class GetEntrantPayments {

    @Test
    public void shouldReturn200StatusCodeWhenParamsAreValid() throws Exception {
      String payload = requestWithVrns(Arrays.asList("CAS123", "CAS124"));
      mockValidGetPaidEntrantPaymentScenario();

      performRequestWithContent(payload)
          .andExpect(status().isOk())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));

      verify(getPaidEntrantPaymentsService).getResults(any(), any(), any(), any());
    }

    @Test
    public void shouldReturn400StatusCodeCleanAirZoneIdIsNull() throws Exception {
      String payload = requestWithoutCleanAirZoneId();

      performRequestWithContent(payload)
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(jsonPath("message").value("cleanAirZoneId cannot be null."));

      verify(getPaidEntrantPaymentsService, never()).getResults(any(), any(), any(), any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenVrnsIsNull() throws Exception {
      String payload = requestWithoutVrns();

      performRequestWithContent(payload)
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(jsonPath("message").value("VRNs cannot be blank."));

      verify(getPaidEntrantPaymentsService, never()).getResults(any(), any(), any(), any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenStartDateIsNull() throws Exception {
      String payload = requestWithoutStartDate();

      performRequestWithContent(payload)
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(jsonPath("message").value("startDate cannot be null."));

      verify(getPaidEntrantPaymentsService, never()).getResults(any(), any(), any(), any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenEndDateIsNull() throws Exception {
      String payload = requestWithoutEndDate();

      performRequestWithContent(payload)
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(jsonPath("message").value("endDate cannot be null."));

      verify(getPaidEntrantPaymentsService, never()).getResults(any(), any(), any(), any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenCorrelationIdIsMissing() throws Exception {
      String payload = requestWithVrns(Arrays.asList("CAS123"));

      mockMvc.perform(post(GET_PAID_PATH).content(payload)
          .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("message").value("Missing request header 'X-Correlation-ID'"));

      verify(getPaidEntrantPaymentsService, never()).getResults(any(), any(), any(), any());
    }

    private ResultActions performRequestWithContent(String payload) throws Exception {
      return mockMvc.perform(post(GET_PAID_PATH).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON));
    }

    private String requestWithoutCleanAirZoneId() {
      PaidPaymentsRequest request = basePaidPaymentsResultBuilder().cleanAirZoneId(null).build();

      return toJsonString(request);
    }

    private String requestWithVrns(List<String> vrns) {
      PaidPaymentsRequest request = basePaidPaymentsResultBuilder().vrns(vrns).build();

      return toJsonString(request);
    }

    private String requestWithoutVrns() {
      PaidPaymentsRequest request = basePaidPaymentsResultBuilder().vrns(null).build();

      return toJsonString(request);
    }

    private String requestWithoutStartDate() {
      PaidPaymentsRequest request = basePaidPaymentsResultBuilder().startDate(null).build();

      return toJsonString(request);
    }

    private String requestWithoutEndDate() {
      PaidPaymentsRequest request = basePaidPaymentsResultBuilder().endDate(null).build();

      return toJsonString(request);
    }

    private PaidPaymentsRequest.PaidPaymentsRequestBuilder basePaidPaymentsResultBuilder() {
      return PaidPaymentsRequest.builder()
          .cleanAirZoneId(TestObjectFactory.anyCleanAirZoneId())
          .vrns(Arrays.asList("CAS123", "CAS124"))
          .startDate(LocalDate.of(2020, 1, 1))
          .endDate(LocalDate.of(2020, 2, 1));
    }

    private void mockValidGetPaidEntrantPaymentScenario() {
      EntrantPayment validEntrantPayment = EntrantPayments
          .anyPaid()
          .toBuilder()
          .travelDate(LocalDate.of(2020, 1, 1))
          .build();

      Map<String, List<EntrantPayment>> result = ImmutableMap
          .of("CAS123", Arrays.asList(validEntrantPayment));

      given(getPaidEntrantPaymentsService.getResults(any(), any(), any(), any()))
          .willReturn(result);
    }
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
