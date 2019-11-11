package uk.gov.caz.psr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.psr.controller.PaymentsController.BASE_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.service.GetAndUpdatePaymentsService;
import uk.gov.caz.psr.service.InitiatePaymentService;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ContextConfiguration(classes = {GlobalExceptionHandlerConfiguration.class, Configuration.class,
    PaymentsController.class})
@WebMvcTest
class PaymentsControllerTest {
  @MockBean
  private InitiatePaymentService initiatePaymentService;

  @MockBean
  private GetAndUpdatePaymentsService getAndUpdatePaymentsService;

  @MockBean
  private ExternalPaymentsRepository externalPaymentsRepository;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final List<LocalDate> days = Arrays
      .asList(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3));

  private static final String ANY_CORRELATION_ID = UUID.randomUUID().toString();

  @Test
  public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
    String payload = "";

    mockMvc.perform(post(BASE_PATH)
        .content(payload)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Missing request header 'X-Correlation-ID'"));
    verify(initiatePaymentService, never()).createPayment(any());
  }

  @Test
  public void emptyDaysShouldResultIn400() throws Exception {
    String payload = paymentRequestWithEmptyDays();

    mockMvc.perform(post(BASE_PATH)
        .content(payload)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isBadRequest());
    verify(initiatePaymentService, never()).createPayment(any());
  }

  @Test
  public void invalidVrnShouldResultIn400() throws Exception {
    String payload = paymentRequestWithVrn("1234567890123456");

    mockMvc.perform(post(BASE_PATH)
        .content(payload)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isBadRequest());
    verify(initiatePaymentService, never()).createPayment(any());
  }

  @Test
  public void invalidAmountShouldResultIn400() throws Exception {
    String payload = paymentRequestWithAmount(-1250);

    mockMvc.perform(post(BASE_PATH)
        .content(payload)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isBadRequest());
    verify(initiatePaymentService, never()).createPayment(any());
  }

  @Test
  public void invalidReturnUrlShouldResultIn400() throws Exception {
    String payload = paymentRequestWithEmptyReturnUrl();

    mockMvc.perform(post(BASE_PATH)
        .content(payload)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isBadRequest());
    verify(initiatePaymentService, never()).createPayment(any());
  }

  @Test
  public void amountNotDivisibleByNumberOfDaysShouldResultIn400() throws Exception {
    String payload = paymentRequestWithAmountNotDivisibleByNumberOfDays();

    mockMvc.perform(post(BASE_PATH)
        .content(payload)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isBadRequest());
    verify(initiatePaymentService, never()).createPayment(any());
  }

  @Test
  public void shouldReturnValidResponse() throws Exception {
    InitiatePaymentRequest requestParams = baseRequestBuilder().build();
    String payload = toJsonString(requestParams);
    Payment successfullyCreatedPayment = Payments.existing();

    given(initiatePaymentService.createPayment(requestParams))
        .willReturn(successfullyCreatedPayment);

    mockMvc.perform(post(BASE_PATH)
        .content(payload)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.paymentId").value(successfullyCreatedPayment.getId().toString()))
        .andExpect(jsonPath("$.nextUrl").value(successfullyCreatedPayment.getNextUrl()));

    verify(initiatePaymentService).createPayment(requestParams);
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

  private String paymentRequestWithAmountNotDivisibleByNumberOfDays() {
    // amount should be an odd number
    InitiatePaymentRequest requestParams = baseRequestBuilder().amount(501).build();
    return toJsonString(requestParams);
  }

  @SneakyThrows
  private String toJsonString(InitiatePaymentRequest requestParams) {
    return objectMapper.writeValueAsString(requestParams);
  }

  private InitiatePaymentRequest.InitiatePaymentRequestBuilder baseRequestBuilder() {
    return InitiatePaymentRequest.builder()
        .cleanAirZoneId(UUID.randomUUID())
        .days(days)
        .vrn("TEST123")
        .amount(1050)
        .returnUrl("https://example.return.url");
  }
}