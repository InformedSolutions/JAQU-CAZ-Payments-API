package uk.gov.caz.psr.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.web.context.support.WebApplicationContextUtils;

import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.psr.Application;
import uk.gov.caz.psr.service.audit.PaymentDataCleanupService;

/**
 * Lambda function which handle cleaning old audit data.
 */
public class CleanupOldAuditDataLambdaHandler implements RequestStreamHandler {

  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  private PaymentDataCleanupService dataCleanupService;


  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    initializeHandlerIfNull();
    dataCleanupService.cleanupData();
  }

  private void initializeHandlerIfNull() {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
      dataCleanupService = getBean(handler, PaymentDataCleanupService.class);
    }
  }

  private <T> T getBean(SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> exampleServiceClass) {
    return WebApplicationContextUtils
        .getWebApplicationContext(handler.getServletContext()).getBean(exampleServiceClass);
  }
}