package uk.gov.caz.psr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.psr.controller.CsvExportController.CSV_EXPORT_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.service.generatecsv.PaymentsHistoryCsvFileSupervisor;

@ContextConfiguration(classes = {Configuration.class, CsvExportController.class})
@WebMvcTest
class CsvExportControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PaymentsHistoryCsvFileSupervisor paymentsHistoryCsvFileSupervisor;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String ANY_CORRELATION_ID = "79b7a48f-27c7-4947-bd1c-670f981843ef";
  private static final UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID ANY_ACCOUNT_USER_ID = UUID.randomUUID();

  @Test
  public void shouldReturnStatus400WhenCorrelationHeaderIsMissing() throws Exception {
    mockMvc
        .perform(post(CSV_EXPORT_PATH, ANY_ACCOUNT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void shouldReturnStatus400WhenAccountUserIdHasWrongFormat() throws Exception {
    mockMvc
        .perform(post(CSV_EXPORT_PATH, ANY_ACCOUNT_ID)
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .param("accountUserId", "invalid-uuid"))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void shouldReturnStatus200WhenRequestIsValidAndAccountUserIdIsPresent() throws Exception {
    mockCsvFileGeneration();
    mockMvc
        .perform(post(CSV_EXPORT_PATH, ANY_ACCOUNT_ID)
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .param("accountUserId", ANY_ACCOUNT_USER_ID.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fileUrl").isNotEmpty())
        .andExpect(jsonPath("$.bucketName").isNotEmpty());
  }

  @Test
  public void shouldReturnStatus200WhenRequestIsValidAndAccountUserIdIsMissing() throws Exception {
    mockCsvFileGeneration();
    mockMvc
        .perform(post(CSV_EXPORT_PATH, ANY_ACCOUNT_ID)
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fileUrl").isNotEmpty())
        .andExpect(jsonPath("$.bucketName").isNotEmpty());
  }

  private void mockCsvFileGeneration() throws MalformedURLException {
    when(paymentsHistoryCsvFileSupervisor.uploadCsvFileAndGetPresignedUrl(any(), any()))
        .thenReturn(new URL("https://s3.bucket/any.csv"));
  }
}