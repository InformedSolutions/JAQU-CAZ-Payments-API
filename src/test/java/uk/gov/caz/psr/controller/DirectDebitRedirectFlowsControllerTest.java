package uk.gov.caz.psr.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.directdebit.CompleteMandateCreationRequest;
import uk.gov.caz.psr.service.directdebit.DirectDebitMandatesService;

@ContextConfiguration(classes = {ExceptionController.class, Configuration.class,
    DirectDebitRedirectFlowsController.class})
@WebMvcTest
class DirectDebitRedirectFlowsControllerTest {

  private static final String ANY_CORRELATION_ID = "1f7c6a8c-15a3-11ea-b483-afe9911b08f0";

  @MockBean
  private DirectDebitMandatesService directDebitMandatesService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Nested
  class WhenCorrelationIdHeaderIsMissing {

    @Test
    public void shouldResultIn400AndValidMessage()
        throws Exception {
      String payload = "";

      mockMvc.perform(post(DirectDebitRedirectFlowsController.BASE_PATH, "some-flow-id")
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("Missing request header 'X-Correlation-ID'"));
    }
  }

  @Nested
  class WhenCleanAirZoneIdIsEmpty {

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "     "})
    public void shouldResultIn400AndValidMessage(String cleanAirZoneId) throws Exception {
      String payload = buildPayloadWithCleanAirZoneIdEqualTo(cleanAirZoneId);

      mockMvc.perform(post(DirectDebitRedirectFlowsController.BASE_PATH, "some-flow-id")
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("'cleanAirZoneId' should be a valid UUID"));
    }
  }

  @Nested
  class WhenCleanAirZoneIdIsNull {

    @Test
    public void shouldResultIn400AndValidMessage() throws Exception {
      String cleanAirZoneId = null;
      String payload = buildPayloadWithCleanAirZoneIdEqualTo(cleanAirZoneId);

      mockMvc.perform(post(DirectDebitRedirectFlowsController.BASE_PATH, "some-flow-id")
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("'cleanAirZoneId' should be a valid UUID"));
    }
  }

  @Nested
  class WhenCleanAirZoneIdIsInvalidUuid {

    @ParameterizedTest
    @ValueSource(strings = {"a", "invalid-uuid"})
    public void shouldResultIn400AndValidMessage(String cleanAirZoneId) throws Exception {
      String payload = buildPayloadWithCleanAirZoneIdEqualTo(cleanAirZoneId);

      mockMvc.perform(post(DirectDebitRedirectFlowsController.BASE_PATH, "some-flow-id")
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("'cleanAirZoneId' should be a valid UUID"));
    }
  }

  @Nested
  class WhenSessionTokenIsEmpty {

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "     "})
    public void shouldResultIn400AndValidMessage(String sessionToken) throws Exception {
      String payload = buildPayloadWithSessionTokenEqualTo(sessionToken);

      mockMvc.perform(post(DirectDebitRedirectFlowsController.BASE_PATH, "some-flow-id")
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("'sessionToken' cannot be null or empty"));
    }
  }

  @Nested
  class WhenSessionTokenIsNull {

    @Test
    public void shouldResultIn400AndValidMessage() throws Exception {
      String payload = buildPayloadWithSessionTokenEqualTo(null);

      mockMvc.perform(post(DirectDebitRedirectFlowsController.BASE_PATH, "some-flow-id")
          .content(payload)
          .contentType(MediaType.APPLICATION_JSON)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
          .value("'sessionToken' cannot be null or empty"));
    }
  }

  @SneakyThrows
  private String buildPayloadWithSessionTokenEqualTo(String sessionToken) {
    return objectMapper.writeValueAsString(
        CompleteMandateCreationRequest.builder()
            .sessionToken(sessionToken)
            .build()
    );
  }

  @SneakyThrows
  private String buildPayloadWithCleanAirZoneIdEqualTo(String cleanAirZoneId) {
    return objectMapper.writeValueAsString(
        CompleteMandateCreationRequest.builder()
            .sessionToken("session-token")
            .cleanAirZoneId(cleanAirZoneId)
            .build()
    );
  }
}