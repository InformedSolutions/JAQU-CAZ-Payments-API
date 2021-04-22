package uk.gov.caz.psr.service.generatecsv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Response;
import uk.gov.caz.psr.dto.PaymentsHistoryLambdaInput;
import uk.gov.caz.psr.dto.accounts.UpdatePaymentHistoryExportRequest;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

@ExtendWith(MockitoExtension.class)
public class PaymentHistoryExportServiceTest {

  private static final UUID ANY_CORRELATION_ID = UUID.randomUUID();
  private static final UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private static final Integer ANY_REGISTER_JOB_ID = RandomUtils.nextInt();
  private static final List<UUID> ANY_ACCOUNT_USER_IDS =
      Collections.singletonList(UUID.randomUUID());

  @Mock
  private PaymentsHistoryCsvFileSupervisor paymentsHistoryCsvFileSupervisor;

  @Mock
  private AccountsRepository accountsRepository;

  @InjectMocks
  private PaymentHistoryExportService paymentHistoryExportService;

  @SneakyThrows
  @Test
  public void shouldUploadFileAndCallAccountsApi() {
    //given
    UpdatePaymentHistoryExportRequest request = mockUpdatePaymentHistoryExportRequest();
    when(accountsRepository.updatePaymentHistoryExportJobSync(
        ANY_ACCOUNT_ID, ANY_REGISTER_JOB_ID, request))
        .thenReturn(Response.success(null));

    //when
    paymentHistoryExportService.execute(mockPaymentsHistoryLambdaInput());

    // then
    verify(accountsRepository).updatePaymentHistoryExportJobSync(
        eq(ANY_ACCOUNT_ID), eq(ANY_REGISTER_JOB_ID), any());
  }

  @Test
  public void shouldThrowExternalServiceCallExceptionOnUnsuccessfulResponse()
      throws MalformedURLException {
    //given
    UpdatePaymentHistoryExportRequest request = mockUpdatePaymentHistoryExportRequest();
    when(accountsRepository.updatePaymentHistoryExportJobSync(
        ANY_ACCOUNT_ID, ANY_REGISTER_JOB_ID, request))
        .thenReturn(Response.error(500,
            ResponseBody.create(MediaType.get("application/json"), "")));

    //when
    Throwable throwable = catchThrowable(() -> paymentHistoryExportService
        .execute(mockPaymentsHistoryLambdaInput()));

    // then
    assertThat(throwable).isInstanceOf(ExternalServiceCallException.class);
  }

  private UpdatePaymentHistoryExportRequest mockUpdatePaymentHistoryExportRequest()
      throws MalformedURLException {
    URL fileUrl = mockUrl();
    when(paymentsHistoryCsvFileSupervisor.uploadCsvFileAndGetPresignedUrl(
        ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_IDS)).thenReturn(fileUrl);
    return mockPaymentHistoryExportRequest(fileUrl);
  }

  private URL mockUrl() throws MalformedURLException {
    return new URL("https://return-url.com");
  }

  private UpdatePaymentHistoryExportRequest mockPaymentHistoryExportRequest(URL fileUrl) {
    return UpdatePaymentHistoryExportRequest.builder()
        .fileUrl(fileUrl)
        .status("FINISHED_SUCCESS")
        .build();
  }

  private PaymentsHistoryLambdaInput mockPaymentsHistoryLambdaInput() {
    return PaymentsHistoryLambdaInput.builder()
        .correlationId(ANY_CORRELATION_ID)
        .registerJobId(ANY_REGISTER_JOB_ID)
        .accountId(ANY_ACCOUNT_ID)
        .accountUserIds(ANY_ACCOUNT_USER_IDS)
        .build();
  }
}
