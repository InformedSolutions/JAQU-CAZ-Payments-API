package uk.gov.caz.psr.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.psr.Application;
import uk.gov.caz.psr.service.CleanupDanglingPaymentsService;

public class CleanupDanglingPaymentsHandler implements RequestStreamHandler {

  private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
    initializeHandlerIfNull();
    CleanupDanglingPaymentsService service = getBean(handler, CleanupDanglingPaymentsService.class);
    service.updateStatusesOfDanglingPayments();
  }

  private void initializeHandlerIfNull() {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
    }
  }

  private <T> T getBean(SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> beanClass) {
    return WebApplicationContextUtils.getWebApplicationContext(handler.getServletContext())
        .getBean(beanClass);
  }
}