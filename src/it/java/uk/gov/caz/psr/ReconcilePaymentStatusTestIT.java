package uk.gov.caz.psr;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.controller.PaymentsController;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.TestObjectFactory;

@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@IntegrationTest
@AutoConfigureMockMvc
public class ReconcilePaymentStatusTestIT {

  private static final String URL_TEMPLATE = PaymentsController.BASE_PATH + "/{id}";
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PaymentRepository paymentRepository;
  
  @BeforeEach
  public void init() throws JsonProcessingException {
    ObjectMapper om = new ObjectMapper();
  }

  @ParameterizedTest
  @ValueSource(strings = {"a", "1111", "a-1-b-2"})
  public void shouldReturn400StatusWhenIdHasInvalidFormat(String id) throws Exception {
    String correlationId = "31f69f26-fb99-11e9-8483-9fcf0b2b434f";
    mockMvc
        .perform(put(URL_TEMPLATE, id)
            .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
            .header("content-type", MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, correlationId))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void shouldReturn404StatusWhenPaymentDoesNotExistInDatabase() throws Exception {
    UUID notExistingId = UUID.fromString("8916a4e0-fb9e-11e9-8483-b3fce09a0a00");

    String correlationId = "542de1b5-4aab-45eb-bccc-6ec91f1d6d51";
    mockMvc
        .perform(put(URL_TEMPLATE, notExistingId)
            .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
            .header("content-type", MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, correlationId))
        .andExpect(status().isNotFound());
  }

  @Test
  public void shouldReturn404StatusWhenPaymentWithoutExtIdExistInDatabase() throws Exception {
    UUID paymentId = insertIntoDatabasePaymentWithoutExternalId();

    String correlationId = "939898b0-fb9e-11e9-8483-cb50ccd05275";
    mockMvc
        .perform(put(URL_TEMPLATE, paymentId)
            .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
            .header("content-type", MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, correlationId))
        .andExpect(status().isNotFound());
  }

  private UUID insertIntoDatabasePaymentWithoutExternalId() {
    Payment withoutId = TestObjectFactory.Payments.forRandomDays().toBuilder()
        .externalPaymentStatus(ExternalPaymentStatus.INITIATED)
        .entrantPayments(Collections.emptyList()).build();
    return paymentRepository.insert(withoutId).getId();
  }
}
