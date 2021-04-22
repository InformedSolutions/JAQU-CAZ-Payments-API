package uk.gov.caz.psr.service.generatecsv;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gocardless.resources.RedirectFlow;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import retrofit2.Response;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse;
import uk.gov.caz.psr.dto.PaymentsHistoryLambdaInput;
import uk.gov.caz.psr.dto.accounts.UpdatePaymentHistoryExportRequest;
import uk.gov.caz.psr.repository.AccountsRepository;

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
    mockServices();

    //when
    paymentHistoryExportService.execute(mockPaymentsHistoryLambdaInput());

    // then
    verify(accountsRepository).updatePaymentHistoryExportJobSync(
        eq(ANY_ACCOUNT_ID), eq(ANY_REGISTER_JOB_ID), any());
  }

  private PaymentsHistoryLambdaInput mockPaymentsHistoryLambdaInput() {
    return PaymentsHistoryLambdaInput.builder()
        .correlationId(ANY_CORRELATION_ID)
        .registerJobId(ANY_REGISTER_JOB_ID)
        .accountId(ANY_ACCOUNT_ID)
        .accountUserIds(ANY_ACCOUNT_USER_IDS)
        .build();
  }

  private void mockServices() throws MalformedURLException {
    URL fileUrl = new URL("https://return-url.com");

    when(paymentsHistoryCsvFileSupervisor.uploadCsvFileAndGetPresignedUrl(
        ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_IDS)).thenReturn(fileUrl);

    UpdatePaymentHistoryExportRequest request = UpdatePaymentHistoryExportRequest.builder()
        .fileUrl(fileUrl)
        .status("FINISHED_SUCCESS")
        .build();

    when(accountsRepository.updatePaymentHistoryExportJobSync(
        ANY_ACCOUNT_ID, ANY_REGISTER_JOB_ID, request))
        .thenReturn(Response.success(null));
  }
}
