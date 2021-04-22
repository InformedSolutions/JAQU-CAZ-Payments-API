package uk.gov.caz.psr.service.generatecsv;

import java.net.URL;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;
import uk.gov.caz.psr.dto.PaymentsHistoryLambdaInput;
import uk.gov.caz.psr.dto.accounts.UpdatePaymentHistoryExportRequest;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;
import uk.gov.caz.psr.util.ResponseBodyUtils;

@Slf4j
public class PaymentHistoryExportService {

  private AccountsRepository accountsRepository;
  private PaymentsHistoryCsvFileSupervisor paymentsHistoryCsvFileSupervisor;

  /**
   * Upload csv file to s3 and call accounts api to finalise export process.
   * @param request A lambda input which need to be used to upload a csv file and call account api.
   */
  public void execute(PaymentsHistoryLambdaInput request)  {
    URL fileUrl = paymentsHistoryCsvFileSupervisor
        .uploadCsvFileAndGetPresignedUrl(request.getAccountId(), request.getAccountUserIds());
    UpdatePaymentHistoryExportRequest patchBody = paymentHistoryExportRequest(fileUrl);
    Response<Void> response = accountsRepository.updatePaymentHistoryExportJobSync(
        request.getAccountId(), request.getRegisterJobId(), patchBody);
    if (!response.isSuccessful()) {
      throw new ExternalServiceCallException(String.format("Accounts service call failed, status "
          + "code: %s, error body: %s", response.code(), getErrorBody(response)));
    }
  }

  private UpdatePaymentHistoryExportRequest paymentHistoryExportRequest(URL fileUrl) {
    return UpdatePaymentHistoryExportRequest.builder()
        .fileUrl(fileUrl)
        .status("FINISHED_SUCCESS")
        .build();
  }

  /**
   * Quietly returns contents of the error body.
   */
  private <T> String getErrorBody(Response<T> response) {
    return ResponseBodyUtils.readQuietly(response.errorBody());
  }
}
