package uk.gov.caz.psr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.psr.controller.PaymentsController.BASE_PATH;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import uk.gov.caz.psr.dto.Transaction;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.GetPaidEntrantPaymentsService;
import uk.gov.caz.psr.service.InitiatePaymentService;
import uk.gov.caz.psr.service.PaymentService;
import uk.gov.caz.psr.service.ReconcilePaymentStatusService;
import uk.gov.caz.psr.util.InitiatePaymentRequestToModelConverter;
import uk.gov.caz.psr.util.PaymentDetailsConverter;
import uk.gov.caz.psr.util.PaymentTransactionsToEntrantsConverter;
import uk.gov.caz.psr.util.ReferencesHistoryConverter;
import uk.gov.caz.psr.util.TestObjectFactory;
import uk.gov.caz.psr.util.TestObjectFactory.EntrantPayments;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ContextConfiguration(classes = {ExceptionController.class, Configuration.class,
    PaymentsController.class})
@WebMvcTest
class PaymentsControllerTest {

  @MockBean
  private PaymentService paymentService;

  @MockBean
  private PaymentDetailsConverter paymentDetailsConverter;

  @MockBean
  private ReferencesHistoryConverter referencesHistoryConverter;

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

  private static final Transaction ANY_TRANSACTION =
      Transaction.builder().charge(100).tariffCode("tariff-code-1")
          .travelDate(LocalDate.of(2019, 1, 1)).vrn("some-vrn").build();

  private static final String ANY_CORRELATION_ID = UUID.randomUUID().toString();

  private static final String GET_PAID_PATH = PaymentsController.BASE_PATH + "/"
      + PaymentsController.GET_PAID_VEHICLE_ENTRANTS;

  private static final String GET_REFERENCES_HISTORY = PaymentsController.BASE_PATH + "/"
      + PaymentsController.GET_REFERENCES_HISTORY;

  @Nested
  class InitiatePayment {

    @Test
    public void missingCorrelationIdShouldResultIn400AndValidMessage()
        throws Exception {
      String payload = "";

      mockMvc
          .perform(post(BASE_PATH).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("Missing request header 'X-Correlation-ID'"));
      verify(initiatePaymentService, never()).createPayment(any(), anyList(),
          any());
    }

    @Test
    public void emptyTransactionsShouldResultIn400() throws Exception {
      String payload = paymentRequestWithEmptyTransactions();

      executeRequest(payload)
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("'transactions' cannot be null or empty"));
      verify(initiatePaymentService, never()).createPayment(any(), anyList(),
          any());
    }

    @Test
    public void lackOfTelephonePaymentShouldResultIn400() throws Exception {
      String payload = paymentRequestWithoutTelephonePayment();

      executeRequest(payload)
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("'telephonePayment' cannot be null"));
      verify(initiatePaymentService, never()).createPayment(any(), anyList(),
          any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890123456", ""})
    public void invalidVrnShouldResultIn400(String vrn) throws Exception {
      String payload = paymentRequestWithVrn(vrn);

      executeRequest(payload)
          .andExpect(status().isBadRequest());
      verify(initiatePaymentService, never()).createPayment(any(), anyList(),
          any());
    }

    @Test
    public void invalidChargeShouldResultIn400() throws Exception {
      String payload = paymentRequestWithAmount(-1250);

      executeRequest(payload)
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("'charge' in all transactions must be positive"));
      verify(initiatePaymentService, never()).createPayment(any(), anyList(),
          any());
    }

    @Test
    public void duplicatedTransactionsShouldResultIn400() throws Exception {
      String payload = paymentRequestWithDuplicatedTransactions();

      executeRequest(payload)
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("Request cannot have duplicated travel date(s)"));
      verify(initiatePaymentService, never()).createPayment(any(), anyList(),
          any());
    }

    @Test
    public void invalidReturnUrlShouldResultIn400() throws Exception {
      String payload = paymentRequestWithEmptyReturnUrl();

      executeRequest(payload)
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("'returnUrl' cannot be null or empty"));

      verify(initiatePaymentService, never()).createPayment(any(), anyList(),
          any());
    }

    @Test
    public void invalidTariffCodeShouldResultIn400() throws Exception {
      String payload = paymentRequestWithEmptyTariffCode();

      executeRequest(payload)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value(
              "'tariffCode' in all transactions cannot be null or empty"));
      verify(initiatePaymentService, never()).createPayment(any(), anyList(),
          any());
    }

    private ResultActions executeRequest(String payload) throws Exception {
      return mockMvc
          .perform(post(BASE_PATH).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
    }

    @Nested
    class WhenOperatorIdIsPresentAndTelephonePaymentIsFalse {

      @Test
      public void shouldResultIn400StatusCodeResponse() throws Exception {
        String payload = paymentRequestWithOperatorIdAndTelephonePaymentSetTo(
            UUID.randomUUID().toString(), false
        );

        executeRequest(payload)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "'operatorId' cannot be set when 'telephonePayment' is false"));
        verify(initiatePaymentService, never()).createPayment(any(), anyList(),
            any());
      }
    }

    @Nested
    class WhenContainsMalformedOperatorId {

      @ParameterizedTest
      @ValueSource(
          strings = {"24d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8", "malformed-uuid"})
      public void shouldResultIn400(String operatorId) throws Exception {
        String payload = paymentRequestWithMalformedOperatorId(operatorId);

        executeRequest(payload)
            .andExpect(status().isBadRequest()).andExpect(
            jsonPath("$.message").value("'operatorId' must be a valid UUID"));
        verify(initiatePaymentService, never()).createPayment(any(), anyList(), any());
      }
    }

    @Nested
    class WhenContainsMalformedUserId {

      @ParameterizedTest
      @ValueSource(
          strings = {"24d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8", "malformed-uuid"})
      public void shouldResultIn400(String userId) throws Exception {
        String payload = paymentRequestWithMalformedUserId(userId);

        executeRequest(payload)
            .andExpect(status().isBadRequest()).andExpect(
            jsonPath("$.message").value("'userId' must be a valid UUID"));
        verify(initiatePaymentService, never()).createPayment(any(), anyList(), any());
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"7caf9cb5-839a-44af-b970-4546bdc80c61", ""})
    public void shouldReturnValidResponse(String userId) throws Exception {
      InitiatePaymentRequest requestParams = correctPaymentRequestWithUserId(userId);
      String payload = toJsonString(requestParams);
      Payment successfullyCreatedPayment = Payments.existing();
      mockSuccessfulInvocationOfInitPaymentService(requestParams,
          successfullyCreatedPayment);

      executeRequest(payload)
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.paymentId")
              .value(successfullyCreatedPayment.getId().toString()))
          .andExpect(jsonPath("$.nextUrl")
              .value(successfullyCreatedPayment.getNextUrl()));

      verify(initiatePaymentService).createPayment(
          InitiatePaymentRequestToModelConverter.toPayment(requestParams),
          PaymentTransactionsToEntrantsConverter.toSingleEntrantPayments(
              requestParams.getTransactions()),
          requestParams.getReturnUrl());
    }

    private void mockSuccessfulInvocationOfInitPaymentService(
        InitiatePaymentRequest requestParams,
        Payment successfullyCreatedPayment) {
      given(initiatePaymentService.createPayment(
          InitiatePaymentRequestToModelConverter.toPayment(requestParams),
          PaymentTransactionsToEntrantsConverter.toSingleEntrantPayments(
              requestParams.getTransactions()),
          requestParams.getReturnUrl())).willReturn(successfullyCreatedPayment);
    }

    private InitiatePaymentRequest correctPaymentRequestWithUserId(String userId) {
      return correctPaymentRequest()
          .toBuilder()
          .userId(userId)
          .build();
    }

    private InitiatePaymentRequest correctPaymentRequest() {
      return baseRequestBuilder()
          .userId(UUID.randomUUID().toString())
          .telephonePayment(Boolean.FALSE)
          .transactions(Collections.singletonList(Transaction.builder()
                  .tariffCode("tariff")
                  .vrn("vrn").travelDate(LocalDate.now())
                  .charge(120)
                  .build()
              )
          )
          .build();
    }

    @SneakyThrows
    private String paymentRequestWithoutTelephonePayment() {
      InitiatePaymentRequest requestParams = correctPaymentRequest()
          .toBuilder()
          .telephonePayment(null)
          .build();
      return toJsonStringIncludingNonNullValues(requestParams);
    }

    private ObjectMapper getObjectMapperThatIgnoresNullFields() {
      ObjectMapper om = objectMapper.copy();
      om.setSerializationInclusion(Include.NON_NULL);
      return om;
    }

    private String paymentRequestWithMalformedUserId(String userId) {
      InitiatePaymentRequest requestParams =
          baseRequestBuilder().userId(userId).build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithMalformedOperatorId(String operatorId) {
      return paymentRequestWithOperatorIdAndTelephonePaymentSetTo(operatorId, true);
    }

    private String paymentRequestWithEmptyTransactions() {
      InitiatePaymentRequest requestParams =
          baseRequestBuilder().transactions(Collections.emptyList()).build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithDuplicatedTransactions() {
      InitiatePaymentRequest requestParams = baseRequestBuilder()
          .transactions(Arrays.asList(ANY_TRANSACTION, ANY_TRANSACTION))
          .build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithVrn(String vrn) {
      InitiatePaymentRequest requestParams = baseRequestBuilder()
          .transactions(Collections
              .singletonList(ANY_TRANSACTION.toBuilder().vrn(vrn).build()))
          .build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithAmount(Integer amount) {
      InitiatePaymentRequest requestParams =
          baseRequestBuilder().transactions(Collections.singletonList(
              ANY_TRANSACTION.toBuilder().charge(amount).build())).build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithEmptyReturnUrl() {
      InitiatePaymentRequest requestParams =
          baseRequestBuilder().returnUrl("").build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithEmptyTariffCode() {
      InitiatePaymentRequest requestParams =
          baseRequestBuilder().transactions(Collections.singletonList(
              ANY_TRANSACTION.toBuilder().tariffCode("").build())).build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithOperatorIdAndTelephonePaymentSetTo(
        String operatorId, boolean telephonePayment) {
      InitiatePaymentRequest requestParams =
          baseRequestBuilder()
              .telephonePayment(telephonePayment)
              .operatorId(operatorId)
              .build();
      return toJsonString(requestParams);
    }

    private InitiatePaymentRequest.InitiatePaymentRequestBuilder baseRequestBuilder() {
      return InitiatePaymentRequest.builder()
          .telephonePayment(Boolean.FALSE)
          .transactions(Collections.singletonList(ANY_TRANSACTION))
          .cleanAirZoneId(UUID.randomUUID())
          .returnUrl("https://example.return.url");
    }

    @SneakyThrows
    private String toJsonString(Object o) {
      return objectMapper.writeValueAsString(o);
    }

    @SneakyThrows
    private String toJsonStringIncludingNonNullValues(Object o) {
      return getObjectMapperThatIgnoresNullFields().writeValueAsString(o);
    }
  }

  @Nested
  class GetEntrantPayments {

    @Test
    public void shouldReturn200StatusCodeWhenParamsAreValid() throws Exception {
      String payload = requestWithVrns(Arrays.asList("CAS123", "CAS124"));
      mockValidGetPaidEntrantPaymentScenario();

      performRequestWithContent(payload).andExpect(status().isOk())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER,
              ANY_CORRELATION_ID));

      verify(getPaidEntrantPaymentsService).getResults(any(), any(), any(),
          any());
    }

    @Test
    public void shouldReturn400StatusCodeCleanAirZoneIdIsNull()
        throws Exception {
      String payload = requestWithoutCleanAirZoneId();

      performRequestWithContent(payload).andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER,
              ANY_CORRELATION_ID))
          .andExpect(
              jsonPath("message").value("cleanAirZoneId cannot be null."));

      verify(getPaidEntrantPaymentsService, never()).getResults(any(), any(),
          any(), any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenVrnsIsNull() throws Exception {
      String payload = requestWithoutVrns();

      performRequestWithContent(payload).andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER,
              ANY_CORRELATION_ID))
          .andExpect(jsonPath("message").value("VRNs cannot be blank."));

      verify(getPaidEntrantPaymentsService, never()).getResults(any(), any(),
          any(), any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenStartDateIsNull()
        throws Exception {
      String payload = requestWithoutStartDate();

      performRequestWithContent(payload).andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER,
              ANY_CORRELATION_ID))
          .andExpect(jsonPath("message").value("startDate cannot be null."));

      verify(getPaidEntrantPaymentsService, never()).getResults(any(), any(),
          any(), any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenEndDateIsNull() throws Exception {
      String payload = requestWithoutEndDate();

      performRequestWithContent(payload).andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER,
              ANY_CORRELATION_ID))
          .andExpect(jsonPath("message").value("endDate cannot be null."));

      verify(getPaidEntrantPaymentsService, never()).getResults(any(), any(),
          any(), any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenStartDateIsAfterEndDate()
        throws Exception {
      String payload = requestWithStartDateAfterEndDate();

      performRequestWithContent(payload).andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER,
              ANY_CORRELATION_ID))
          .andExpect(
              jsonPath("message").value("endDate cannot be before startDate."));

      verify(getPaidEntrantPaymentsService, never()).getResults(any(), any(),
          any(), any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenVrnsAreEmpty() throws Exception {
      String payload = requestWithEmptyVrns();

      performRequestWithContent(payload).andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER,
              ANY_CORRELATION_ID))
          .andExpect(jsonPath("message").value("VRNs cannot be empty."));

      verify(getPaidEntrantPaymentsService, never()).getResults(any(), any(),
          any(), any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenCorrelationIdIsMissing()
        throws Exception {
      String payload = requestWithVrns(Arrays.asList("CAS123"));

      mockMvc
          .perform(post(GET_PAID_PATH).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("message")
          .value("Missing request header 'X-Correlation-ID'"));

      verify(getPaidEntrantPaymentsService, never()).getResults(any(), any(),
          any(), any());
    }

    private ResultActions performRequestWithContent(String payload)
        throws Exception {
      return mockMvc.perform(post(GET_PAID_PATH).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }

    private String requestWithoutCleanAirZoneId() {
      PaidPaymentsRequest request =
          basePaidPaymentsResultBuilder().cleanAirZoneId(null).build();

      return toJsonString(request);
    }

    private String requestWithVrns(List<String> vrns) {
      PaidPaymentsRequest request =
          basePaidPaymentsResultBuilder().vrns(vrns).build();

      return toJsonString(request);
    }

    private String requestWithoutVrns() {
      PaidPaymentsRequest request =
          basePaidPaymentsResultBuilder().vrns(null).build();

      return toJsonString(request);
    }

    private String requestWithEmptyVrns() {
      PaidPaymentsRequest request =
          basePaidPaymentsResultBuilder().vrns(Collections.emptyList()).build();

      return toJsonString(request);
    }

    private String requestWithoutStartDate() {
      PaidPaymentsRequest request =
          basePaidPaymentsResultBuilder().startDate(null).build();

      return toJsonString(request);
    }

    private String requestWithoutEndDate() {
      PaidPaymentsRequest request =
          basePaidPaymentsResultBuilder().endDate(null).build();

      return toJsonString(request);
    }

    private String requestWithStartDateAfterEndDate() {
      LocalDate startDate = LocalDate.now();
      PaidPaymentsRequest request = basePaidPaymentsResultBuilder()
          .startDate(startDate).endDate(startDate.minusDays(1)).build();

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
      EntrantPayment validEntrantPayment = EntrantPayments.anyPaid().toBuilder()
          .travelDate(LocalDate.of(2020, 1, 1)).build();

      Map<String, List<EntrantPayment>> result =
          ImmutableMap.of("CAS123", Arrays.asList(validEntrantPayment));

      given(
          getPaidEntrantPaymentsService.getResults(any(), any(), any(), any()))
          .willReturn(result);
    }
  }

  @Nested
  class GetReferencesHistory {

    @Test
    public void shouldReturn404WhenThereIsNoPaymentForGivenReferenceNumber()
        throws Exception {
      given(paymentService.getPaymentByReferenceNumber(any())).willReturn(Optional.empty());

      mockMvc
          .perform(get(GET_REFERENCES_HISTORY, 1200)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());
    }
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
