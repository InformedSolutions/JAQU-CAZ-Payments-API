package uk.gov.caz.psr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.psr.controller.DirectDebitMandatesController.BASE_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.psr.dto.CreateDirectDebitMandateRequest;
import uk.gov.caz.psr.service.directdebit.DirectDebitMandatesService;
import uk.gov.caz.psr.util.CleanAirZoneWithMandatesToDtoConverter;

@ContextConfiguration(classes = {ExceptionController.class, Configuration.class,
    DirectDebitMandatesController.class})
@WebMvcTest
class DirectDebitMandatesControllerTest {

  @MockBean
  private CleanAirZoneWithMandatesToDtoConverter cleanAirZoneWithMandatesToDtoConverter;

  @MockBean
  private DirectDebitMandatesService directDebitMandatesService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  public void resetMocks() {
    Mockito.reset(directDebitMandatesService);
  }

  private static final String VALID_CORRELATION_HEADER = "79b7a48f-27c7-4947-bd1c-670f981843ef";
  private static final UUID ANY_ACCOUNT_ID = UUID.randomUUID();

  @Nested
  class Create {

    @Test
    public void missingCorrelationIdShouldResultIn400AndValidMessage()
        throws Exception {
      String payload = "";

      mockMvc
          .perform(post(BASE_PATH, ANY_ACCOUNT_ID).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message")
              .value("Missing request header 'X-Correlation-ID'"));
      verify(directDebitMandatesService, never())
          .createDirectDebitMandate(any(), any(), any());
    }

    @Test
    public void emptyCAZShouldResultIn400() throws Exception {
      String payload = directDebitMandateRequestWithEmptyCaz();

      mockMvc
          .perform(post(BASE_PATH, ANY_ACCOUNT_ID).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, VALID_CORRELATION_HEADER))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message")
              .value("'cleanAirZoneId' cannot be null"));
      verify(directDebitMandatesService, never())
          .createDirectDebitMandate(any(), any(), any());
    }

    @Test
    public void emptyReturnUrlShouldResultIn400() throws Exception {
      String payload = directDebitMandateRequestWithReturnUrl("");

      mockMvc
          .perform(post(BASE_PATH, ANY_ACCOUNT_ID).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, VALID_CORRELATION_HEADER))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message")
              .value("'returnUrl' cannot be null or empty"));
      verify(directDebitMandatesService, never())
          .createDirectDebitMandate(any(), any(), any());
    }

    @Test
    public void nullReturnUrlShouldResultIn400() throws Exception {
      String payload = directDebitMandateRequestWithReturnUrl(null);

      mockMvc
          .perform(post(BASE_PATH, ANY_ACCOUNT_ID).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, VALID_CORRELATION_HEADER))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message")
              .value("'returnUrl' cannot be null or empty"));
      verify(directDebitMandatesService, never())
          .createDirectDebitMandate(any(), any(), any());
    }

    @Test
    public void validRequestShouldResultIn201() throws Exception {
      String payload = validRequestPayload();

      mockMvc
          .perform(post(BASE_PATH, ANY_ACCOUNT_ID).content(payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header(X_CORRELATION_ID_HEADER, VALID_CORRELATION_HEADER))
          .andExpect(status().isCreated());
      verify(directDebitMandatesService)
          .createDirectDebitMandate(any(), any(), any());
    }

    private CreateDirectDebitMandateRequest.CreateDirectDebitMandateRequestBuilder baseRequestBuilder() {
      return CreateDirectDebitMandateRequest.builder()
          .cleanAirZoneId(UUID.randomUUID())
          .returnUrl("https://example.return.url");
    }

    private String directDebitMandateRequestWithEmptyCaz() {
      CreateDirectDebitMandateRequest requestParams = baseRequestBuilder()
          .cleanAirZoneId(null)
          .build();
      return toJsonString(requestParams);
    }

    private String directDebitMandateRequestWithReturnUrl(String returnUrl) {
      CreateDirectDebitMandateRequest requestParams = baseRequestBuilder()
          .returnUrl(returnUrl)
          .build();
      return toJsonString(requestParams);
    }

    private String validRequestPayload() {
      CreateDirectDebitMandateRequest requestParams = baseRequestBuilder().build();
      return toJsonString(requestParams);
    }
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}