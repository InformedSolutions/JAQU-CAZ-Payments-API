package uk.gov.caz.psr;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.controller.PaymentsController;
import uk.gov.caz.psr.dto.PaidPaymentsRequest;
import uk.gov.caz.psr.dto.PaidPaymentsResponse;
import uk.gov.caz.psr.dto.PaidPaymentsResponse.PaidPaymentsResult;

@Sql(scripts = "classpath:data/sql/add-payments-for-payment-status.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@IntegrationTest
@AutoConfigureMockMvc
public class GetPaidEntrantPaymentsTestIT {

  private static final String PATH =
      PaymentsController.BASE_PATH + "/" + PaymentsController.GET_PAID_VEHICLE_ENTRANTS;

  private static final String VALID_CORRELATION_HEADER = "79b7a48f-27c7-4947-bd1c-670f981843ef";
  private static final String VALID_CAZ_ID = "b8e53786-c5ca-426a-a701-b14ee74857d4";
  private static final String VALID_VRN = "ND84VSX";
  private static final LocalDate VALID_START_DATE = LocalDate.of(2019, 11, 1);
  private static final LocalDate VALID_END_DATE = LocalDate.of(2019, 11, 5);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  public void shouldReturn400WhenParametersIsMissing() throws Exception {
    mockMvc.perform(post(PATH)
        .content(invalidJsonPayload())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(Constants.X_CORRELATION_ID_HEADER, VALID_CORRELATION_HEADER)
        .header("x-api-key", VALID_CAZ_ID))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void shouldReturn200WithResultsWhenRequestIsValid() throws Exception {
    mockMvc.perform(post(PATH)
        .content(validJsonPayload())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(Constants.X_CORRELATION_ID_HEADER, VALID_CORRELATION_HEADER)
        .header("x-api-key", VALID_CAZ_ID))
        .andExpect(status().isOk())
        .andExpect(content().json(validJsonResponse()));
  }

  private String validJsonResponse() {
    PaidPaymentsResponse response = PaidPaymentsResponse.builder()
        .results(Arrays.asList(validResult()))
        .build();

    return toJson(response);
  }

  private PaidPaymentsResult validResult() {
    List<LocalDate> paidDates = Arrays.asList(
        LocalDate.parse("2019-11-01"),
        LocalDate.parse("2019-11-04")
    );

    return PaidPaymentsResult.builder()
        .vrn(VALID_VRN)
        .paidDates(paidDates)
        .build();
  }

  private String validJsonPayload() {
    PaidPaymentsRequest request = PaidPaymentsRequest.builder()
        .vrns(Arrays.asList(VALID_VRN))
        .startDate(VALID_START_DATE)
        .endDate(VALID_END_DATE)
        .build();

    return toJson(request);
  }

  private String invalidJsonPayload() {
    PaidPaymentsRequest request = PaidPaymentsRequest.builder()
        .vrns(null)
        .startDate(null)
        .endDate(null)
        .build();

    return toJson(request);
  }

  @SneakyThrows
  private String toJson(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
