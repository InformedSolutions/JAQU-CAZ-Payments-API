package uk.gov.caz.psr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.psr.controller.AccountsController.ACCOUNTS_PATH;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.psr.controller.util.QueryStringValidator;
import uk.gov.caz.psr.model.EnrichedPaymentSummary;
import uk.gov.caz.psr.model.PaginationData;
import uk.gov.caz.psr.service.AccountService;
import uk.gov.caz.psr.service.ChargeableVehiclesService;
import uk.gov.caz.psr.service.RetrieveSuccessfulPaymentsService;
import uk.gov.caz.psr.util.ChargeableVehiclesToDtoConverter;

@ContextConfiguration(classes = {ExceptionController.class, Configuration.class,
    AccountsController.class})
@WebMvcTest
class AccountsControllerTest {

  @MockBean
  private AccountService accountService;

  @MockBean
  private QueryStringValidator queryStringValidator;

  @MockBean
  private RetrieveSuccessfulPaymentsService retrieveSuccessfulPaymentsService;

  @MockBean
  private ChargeableVehiclesService chargeableVehiclesService;

  @MockBean
  private ChargeableVehiclesToDtoConverter chargeableVehiclesToDtoConverter;

  @Autowired
  private MockMvc mockMvc;

  @Nested
  class RetrieveSuccessfulPayments {

    private static final String ANY_ACCOUNT_ID = "dad034e6-ea18-4a58-a4f0-668a814a766b";
    private static final String ANY_CORRELATION_ID = "fb8f036e-eaf0-42e2-bec2-51589d8018ff";
    private static final String ANY_ACCOUNT_USER_ID = "2375e95c-4db8-44dc-b5aa-a73bc92fbf8a";

    private static final String ANY_PAGE_NUMBER = "1";
    private static final String ANY_PAGE_SIZE = "10";

    private static final String RETRIEVE_SUCCESSFUL_PAYMENTS_PATH =
        ACCOUNTS_PATH + "/{accountId}/payments";

    @Test
    public void shouldReturn400WhenCorrelationIdIsMissing() throws Exception {
      mockMvc.perform(get(RETRIEVE_SUCCESSFUL_PAYMENTS_PATH, ANY_ACCOUNT_ID)
          .accept(MediaType.APPLICATION_JSON)
          .param("accountUserId", ANY_ACCOUNT_USER_ID)
          .param("pageNumber", ANY_PAGE_NUMBER)
          .param("pageSize", ANY_PAGE_SIZE))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn400WhenQueryValidationFails() throws Exception {
      mockFailedQueryValidation();
      performValidRequest().andExpect(status().isBadRequest());
    }

    @Test
    public void shouldRetrieveSingleUserPaymentsWhenAccountUserIdIsPresentAndReturn200()
        throws Exception {
      mockSuccessfulQueryValidation();
      mockSuccessfulSingleUserRetrieval();

      performValidRequest().andExpect(status().isOk());
      verify(retrieveSuccessfulPaymentsService)
          .retrieveForSingleUser(any(), any(), anyInt(), anyInt());
    }

    @Test
    public void shouldRetrieveAllUsersPaymentsWhenAccountUserIdIsMissingAndReturn200()
        throws Exception {
      mockSuccessfulQueryValidation();
      mockSuccessfulAllUsersRetrieval();

      mockMvc.perform(get(RETRIEVE_SUCCESSFUL_PAYMENTS_PATH, ANY_ACCOUNT_ID)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .accept(MediaType.APPLICATION_JSON)
          .param("pageNumber", ANY_PAGE_NUMBER)
          .param("pageSize", ANY_PAGE_SIZE))
          .andExpect(status().isOk());

      verify(retrieveSuccessfulPaymentsService).retrieveForAccount(any(), anyInt(), anyInt());
    }

    private void mockSuccessfulSingleUserRetrieval() {
      when(
          retrieveSuccessfulPaymentsService.retrieveForSingleUser(any(), any(), anyInt(), anyInt()))
          .thenReturn(sampleServiceResult());
    }

    private void mockSuccessfulAllUsersRetrieval() {
      when(retrieveSuccessfulPaymentsService.retrieveForAccount(any(), anyInt(), anyInt()))
          .thenReturn(sampleServiceResult());
    }

    private Pair<PaginationData, List<EnrichedPaymentSummary>> sampleServiceResult() {
      PaginationData paginationData = PaginationData.builder()
          .pageCount(10)
          .pageNumber(0)
          .pageSize(10)
          .totalElementsCount(100)
          .build();

      List<EnrichedPaymentSummary> enrichedPaymentSummaries = Arrays.asList(
          EnrichedPaymentSummary.builder()
              .paymentId(UUID.randomUUID())
              .totalPaid(BigDecimal.valueOf(1000))
              .payerName("Jan Brzechwa")
              .entriesCount(10)
              .cazName("Birmingham")
              .build()
      );

      return Pair.of(paginationData, enrichedPaymentSummaries);
    }

    private ResultActions performValidRequest() throws Exception {
      return mockMvc.perform(get(RETRIEVE_SUCCESSFUL_PAYMENTS_PATH, ANY_ACCOUNT_ID)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .accept(MediaType.APPLICATION_JSON)
          .param("accountUserId", ANY_ACCOUNT_USER_ID)
          .param("pageNumber", ANY_PAGE_NUMBER)
          .param("pageSize", ANY_PAGE_SIZE));
    }

    private void mockSuccessfulQueryValidation() {
      doNothing().when(queryStringValidator).validateRequest(any(), any(), any());
    }

    private void mockFailedQueryValidation() {
      doThrow(InvalidRequestPayloadException.class)
          .when(queryStringValidator).validateRequest(any(), any(), any());
    }
  }
}
