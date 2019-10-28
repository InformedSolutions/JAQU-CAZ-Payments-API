package uk.gov.caz.psr.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.psr.controller.PaymentsController.BASE_PATH;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.service.InitiatePaymentService;

@ContextConfiguration(classes = {GlobalExceptionHandlerConfiguration.class, Configuration.class,
    PaymentsController.class})
@WebMvcTest
class PaymentsControllerTest {
  @MockBean
  private InitiatePaymentService initiatePaymentService;

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
        .andExpect(MockMvcResultMatchers.jsonPath("$.message")
            .value("Missing request header 'X-Correlation-ID'"));
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
  }

  @Test
  @Disabled
  public void shouldReturnValidResponse() throws Exception {
    String payload = paymentRequest();

    mockMvc.perform(post(BASE_PATH)
        .content(payload)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isCreated());
  }

  private String paymentRequest() throws JsonProcessingException {
    InitiatePaymentRequest requestParams = new InitiatePaymentRequest(UUID.randomUUID(), days,
        "TEST123", 1010, "https://example.return.url");
    return objectMapper.writeValueAsString(requestParams);
  }

  private String paymentRequestWithEmptyDays() throws JsonProcessingException {
    List<LocalDate> emptyDays = Collections.emptyList();

    InitiatePaymentRequest requestParams = new InitiatePaymentRequest(UUID.randomUUID(), emptyDays,
        "TEST123", 1050, "https://example.return.url");
    return objectMapper.writeValueAsString(requestParams);
  }

  private String paymentRequestWithVrn(String vrn) throws JsonProcessingException {
    InitiatePaymentRequest requestParams = new InitiatePaymentRequest(UUID.randomUUID(), days,
        vrn, 1050, "https://example.return.url");
    return objectMapper.writeValueAsString(requestParams);
  }

  private String paymentRequestWithAmount(Integer amount) throws JsonProcessingException {
    InitiatePaymentRequest requestParams = new InitiatePaymentRequest(UUID.randomUUID(), days,
        "TEST123", amount, "https://example.return.url");
    return objectMapper.writeValueAsString(requestParams);
  }

  private String paymentRequestWithEmptyReturnUrl() throws JsonProcessingException {
    InitiatePaymentRequest requestParams = new InitiatePaymentRequest(UUID.randomUUID(), days,
        "TEST123", 1050, "");
    return objectMapper.writeValueAsString(requestParams);
  }
}