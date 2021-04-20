package uk.gov.caz.psr.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.common.base.Stopwatch;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.context.support.WebApplicationContextUtils;
import retrofit2.Response;
import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.Application;
import uk.gov.caz.psr.dto.PaymentsHistoryLambdaInput;
import uk.gov.caz.psr.dto.accounts.UpdatePaymentHistoryExportRequest;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;
import uk.gov.caz.psr.service.generatecsv.PaymentsHistoryCsvFileSupervisor;
import uk.gov.caz.psr.util.ResponseBodyUtils;

/**
 * Lambda function that generates CSV file.
 */
@Slf4j
public class PaymentsHistoryLambdaHandler implements
    RequestHandler<PaymentsHistoryLambdaInput, String> {

  private static final String ERROR_BODY = ", error body: '";

  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
  private AccountsRepository accountsRepository;

  @Override
  public String handleRequest(PaymentsHistoryLambdaInput request, Context context) {
    Stopwatch timer = Stopwatch.createStarted();
    request.validate();
    initializeHandlerIfNull();
    log.info("Handler initialization took {}ms", timer.elapsed(TimeUnit.MILLISECONDS));
    try {
      setCorrelationIdInMdc(request.getCorrelationId().toString());
      PaymentsHistoryCsvFileSupervisor paymentsHistoryCsvFileSupervisor = getBean(handler,
          PaymentsHistoryCsvFileSupervisor.class);
      URL fileUrl = paymentsHistoryCsvFileSupervisor
          .uploadCsvFileAndGetPresignedUrl(request.getAccountId(), request.getAccountUserIds());
      UpdatePaymentHistoryExportRequest body = paymentHistoryExportRequest(fileUrl);

      Response<Void> response = accountsRepository.updatePaymentHistoryExportJobSync(
          request.getAccountId(), request.getRegisterJobId(), body);
      if (!response.isSuccessful()) {
        throw new ExternalServiceCallException("Accounts service call failed, status code: "
            + response.code() + ERROR_BODY + getErrorBody(response) + "'");
      }
      return "OK";
    } finally {
      log.info("Exporting payment history method took {}ms",
          timer.stop().elapsed(TimeUnit.MILLISECONDS));
      removeCorrelationIdFromMdc();
    }
  }

  private void initializeHandlerIfNull() {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
    }
  }

  private <T> T getBean(SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> exampleServiceClass) {
    return WebApplicationContextUtils
        .getWebApplicationContext(handler.getServletContext()).getBean(exampleServiceClass);
  }

  private void setCorrelationIdInMdc(String correlationId) {
    MDC.put(Constants.X_CORRELATION_ID_HEADER, correlationId);
  }

  private void removeCorrelationIdFromMdc() {
    MDC.remove(Constants.X_CORRELATION_ID_HEADER);
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
