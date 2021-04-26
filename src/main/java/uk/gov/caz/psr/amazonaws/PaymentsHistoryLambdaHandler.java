package uk.gov.caz.psr.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.Application;
import uk.gov.caz.psr.dto.PaymentsHistoryLambdaInput;
import uk.gov.caz.psr.service.generatecsv.PaymentHistoryExportService;

/**
 * Lambda function that generates CSV file.
 */
@Slf4j
public class PaymentsHistoryLambdaHandler implements
    RequestHandler<PaymentsHistoryLambdaInput, String> {

  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  @Override
  public String handleRequest(PaymentsHistoryLambdaInput request, Context context) {
    Stopwatch timer = Stopwatch.createStarted();
    request.validate();
    initializeHandlerIfNull();
    log.info("Handler initialization took {}ms", timer.elapsed(TimeUnit.MILLISECONDS));
    try {
      setCorrelationIdInMdc(request.getCorrelationId().toString());
      PaymentHistoryExportService service = getBean(handler, PaymentHistoryExportService.class);
      service.execute(request);
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
}
