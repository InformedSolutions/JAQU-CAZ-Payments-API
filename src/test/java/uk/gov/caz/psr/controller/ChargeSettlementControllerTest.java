package uk.gov.caz.psr.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import javax.security.auth.login.Configuration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.correlationid.Constants;

@ContextConfiguration(classes = {Configuration.class, ChargeSettlementController.class})
@WebMvcTest
class ChargeSettlementControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private static final String ANY_VALID_VRN = "DL76MWX";
  private static final String ANY_VALID_DATE_STRING = LocalDate.now().toString();
  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";
  private static final String PAYMENT_STATUS_PATH = ChargeSettlementController.BASE_PATH
      + "/" + ChargeSettlementController.PAYMENT_STATUS_PATH;

  @Nested
  class PaymentStatus {
    @Test
    public void shouldReturn400StatusCodeWhenVrnIsMissing() throws Exception {
      mockMvc.perform(get(PAYMENT_STATUS_PATH)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8)
          .param("dateOfCazEntry", ANY_VALID_DATE_STRING))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn400StatusWhenDateOfCazEntryIsMissing() throws Exception {
      mockMvc.perform(get(PAYMENT_STATUS_PATH)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON_UTF8)
          .accept(MediaType.APPLICATION_JSON_UTF8)
          .param("vrn", ANY_VALID_VRN))
          .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "UNSOPHISTICATION"})
    public void shouldReturn400StatusCodeWhenVrnIsInvalid(String vrn) throws Exception {
      mockMvc.perform(get(PAYMENT_STATUS_PATH)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8)
          .param("vrn", vrn)
          .param("dateOfCazEntry", ANY_VALID_DATE_STRING))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn200StatusCodeWhenRequestIsValid() throws Exception {
      mockMvc.perform(get(PAYMENT_STATUS_PATH)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8)
          .param("vrn", ANY_VALID_VRN)
          .param("dateOfCazEntry", ANY_VALID_DATE_STRING))
          .andExpect(status().isOk());
    }
  }
}
