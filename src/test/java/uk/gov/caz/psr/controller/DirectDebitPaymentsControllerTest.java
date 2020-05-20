package uk.gov.caz.psr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.psr.controller.DirectDebitPaymentsController.BASE_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
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
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.psr.dto.Transaction;
import uk.gov.caz.psr.dto.directdebit.CreateDirectDebitPaymentRequest;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.CreateDirectDebitPaymentService;
import uk.gov.caz.psr.util.DirectDebitPaymentRequestToModelConverter;
import uk.gov.caz.psr.util.PaymentTransactionsToEntrantsConverter;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ContextConfiguration(classes = {ExceptionController.class, Configuration.class,
    DirectDebitPaymentsController.class})
@WebMvcTest
public class DirectDebitPaymentsControllerTest {

  @MockBean
  private CreateDirectDebitPaymentService createDirectDebitPaymentService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  public void resetMocks() {
    Mockito.reset(createDirectDebitPaymentService);
  }

  private static final Transaction ANY_TRANSACTION =
      Transaction.builder().charge(100).tariffCode("tariff-code-1")
          .travelDate(LocalDate.of(2019, 1, 1)).vrn("some-vrn").build();
  private static final String ANY_CORRELATION_ID = UUID.randomUUID().toString();

  @Nested
  class CreatePayment {

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
      verify(createDirectDebitPaymentService, never()).createPayment(any(), anyList());
    }

    @Test
    public void emptyTransactionsShouldResultIn400() throws Exception {
      String payload = paymentRequestWithEmptyTransactions();

      mockMvc
          .perform(post(BASE_PATH).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("'transactions' cannot be null or empty"));
      verify(createDirectDebitPaymentService, never()).createPayment(any(), anyList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890123456", ""})
    public void invalidVrnShouldResultIn400(String vrn) throws Exception {
      String payload = paymentRequestWithVrn(vrn);

      mockMvc
          .perform(post(BASE_PATH).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest());
      verify(createDirectDebitPaymentService, never()).createPayment(any(), anyList());
    }

    @Test
    public void invalidChargeShouldResultIn400() throws Exception {
      String payload = paymentRequestWithAmount(-1250);

      mockMvc
          .perform(post(BASE_PATH).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("'charge' in all transactions must be positive"));
      verify(createDirectDebitPaymentService, never()).createPayment(any(), anyList());
    }

    @Test
    public void duplicatedTransactionsShouldResultIn400() throws Exception {
      String payload = paymentRequestWithDuplicatedTransactions();

      mockMvc
          .perform(post(BASE_PATH).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("Request cannot have duplicated travel date(s)"));
      verify(createDirectDebitPaymentService, never()).createPayment(any(), anyList());
    }

    @Test
    public void invalidMandateIdShouldResultIn400() throws Exception {
      String payload = paymentRequestWithEmptyMandateId();

      mockMvc
          .perform(post(BASE_PATH).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("'mandateId' cannot be null or empty"));

      verify(createDirectDebitPaymentService, never()).createPayment(any(), anyList());
    }

    @Test
    public void invalidTariffCodeShouldResultIn400() throws Exception {
      String payload = paymentRequestWithEmptyTariffCode();

      mockMvc
          .perform(post(BASE_PATH).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value(
              "'tariffCode' in all transactions cannot be null or empty"));
      verify(createDirectDebitPaymentService, never()).createPayment(any(), anyList());
    }

    @Nested
    class WhenContainsMalformedUserId {

      @ParameterizedTest
      @ValueSource(
          strings = {"24d4d8f3b-3b81-44f3-968d-d1c1a48b4ac8", "malformed-uuid"})
      public void shouldResultIn400(String userId) throws Exception {
        String payload = paymentRequestWithMalformedUserId(userId);

        mockMvc
            .perform(post(BASE_PATH).content(payload)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
            .andExpect(status().isBadRequest()).andExpect(
            jsonPath("$.message").value("'userId' must be a valid UUID"));
        verify(createDirectDebitPaymentService, never()).createPayment(any(), anyList());
      }
    }

    @Nested
    class WhenContainsInvalidUserEmail {

      @ParameterizedTest
      @ValueSource(
          strings = {"test", "test@"})
      public void shouldResultIn400(String userEmail) throws Exception {
        String payload = paymentRequestWithInvalidUserEmail(userEmail);

        mockMvc
            .perform(post(BASE_PATH).content(payload)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
            .andExpect(status().isBadRequest()).andExpect(
            jsonPath("$.message").value("'userEmail' is not valid."));
        verify(createDirectDebitPaymentService, never()).createPayment(any(), anyList());
      }

      @Test
      public void forEmptyShouldResultIn400() throws Exception {
        String payload = paymentRequestWithInvalidUserEmail("");

        mockMvc
            .perform(post(BASE_PATH).content(payload)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
            .andExpect(status().isBadRequest()).andExpect(
            jsonPath("$.message").value("'userEmail' cannot be null or empty"));
        verify(createDirectDebitPaymentService, never()).createPayment(any(), anyList());
      }

      @Test
      public void forNullShouldResultIn400() throws Exception {
        String payload = paymentRequestWithInvalidUserEmail(null);

        mockMvc
            .perform(post(BASE_PATH).content(payload)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
            .andExpect(status().isBadRequest()).andExpect(
            jsonPath("$.message").value("'userEmail' cannot be null or empty"));
        verify(createDirectDebitPaymentService, never()).createPayment(any(), anyList());
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"7caf9cb5-839a-44af-b970-4546bdc80c61", ""})
    public void shouldReturnValidResponse(String userId) throws Exception {
      CreateDirectDebitPaymentRequest requestParams = baseRequestBuilder().userId(userId)
          .transactions(Collections
              .singletonList(Transaction.builder().tariffCode("tariff")
                  .vrn("vrn").travelDate(LocalDate.now()).charge(120).build()))
          .build();
      String payload = toJsonString(requestParams);
      Payment successfullyCreatedPayment = Payments.existing().toBuilder()
          .referenceNumber(12345L)
          .build();
      mockSuccessfulInvocationOfCreateDirectDebitPaymentService(requestParams,
          successfullyCreatedPayment);

      mockMvc
          .perform(post(BASE_PATH).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.paymentId")
              .value(successfullyCreatedPayment.getId().toString()))
          .andExpect(jsonPath("$.referenceNumber")
              .value(successfullyCreatedPayment.getReferenceNumber().toString()))
          .andExpect(jsonPath("$.externalPaymentId")
              .value(successfullyCreatedPayment.getExternalId()));

      verify(createDirectDebitPaymentService).createPayment(
          DirectDebitPaymentRequestToModelConverter.toPayment(requestParams),
          PaymentTransactionsToEntrantsConverter.toSingleEntrantPayments(
              requestParams.getTransactions()));
    }

    private String paymentRequestWithEmptyTransactions() {
      CreateDirectDebitPaymentRequest requestParams =
          baseRequestBuilder().transactions(Collections.emptyList()).build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithVrn(String vrn) {
      CreateDirectDebitPaymentRequest requestParams = baseRequestBuilder()
          .transactions(Collections
              .singletonList(ANY_TRANSACTION.toBuilder().vrn(vrn).build()))
          .build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithAmount(Integer amount) {
      CreateDirectDebitPaymentRequest requestParams =
          baseRequestBuilder().transactions(Collections.singletonList(
              ANY_TRANSACTION.toBuilder().charge(amount).build())).build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithDuplicatedTransactions() {
      CreateDirectDebitPaymentRequest requestParams = baseRequestBuilder()
          .transactions(Arrays.asList(ANY_TRANSACTION, ANY_TRANSACTION))
          .build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithEmptyTariffCode() {
      CreateDirectDebitPaymentRequest requestParams =
          baseRequestBuilder().transactions(Collections.singletonList(
              ANY_TRANSACTION.toBuilder().tariffCode("").build())).build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithEmptyMandateId() {
      CreateDirectDebitPaymentRequest requestParams =
          baseRequestBuilder().mandateId("").build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithMalformedUserId(String userId) {
      CreateDirectDebitPaymentRequest requestParams =
          baseRequestBuilder().userId(userId).build();
      return toJsonString(requestParams);
    }

    private String paymentRequestWithInvalidUserEmail(String userEmail) {
      CreateDirectDebitPaymentRequest requestParams =
          baseRequestBuilder().userEmail(userEmail).build();
      return toJsonString(requestParams);
    }

    private CreateDirectDebitPaymentRequest.CreateDirectDebitPaymentRequestBuilder baseRequestBuilder() {
      return CreateDirectDebitPaymentRequest.builder()
          .transactions(Collections.singletonList(ANY_TRANSACTION))
          .cleanAirZoneId(UUID.randomUUID())
          .mandateId("exampleMandateId")
          .userEmail("test@email.com");
    }

    private void mockSuccessfulInvocationOfCreateDirectDebitPaymentService(
        CreateDirectDebitPaymentRequest requestParams,
        Payment successfullyCreatedPayment) {
      given(createDirectDebitPaymentService.createPayment(
          DirectDebitPaymentRequestToModelConverter.toPayment(requestParams),
          PaymentTransactionsToEntrantsConverter.toSingleEntrantPayments(
              requestParams.getTransactions()))).willReturn(successfullyCreatedPayment);
    }

    @SneakyThrows
    private String toJsonString(CreateDirectDebitPaymentRequest requestParams) {
      return objectMapper.writeValueAsString(requestParams);
    }
  }
}
