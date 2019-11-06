package uk.gov.caz.psr.amazonaws;

import static uk.gov.caz.awslambda.AwsHelpers.splitToArray;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import uk.gov.caz.psr.Application;

public class StreamLambdaHandler implements RequestStreamHandler {

  private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  private static final int INITIALIZATION_TIMEOUT_IN_MS = 60_000;

  static {
    long startTime = Instant.now().toEpochMilli();
    try {
      // For applications that take longer than 10 seconds to start, use the async builder:
      String listOfActiveSpringProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
      LambdaContainerHandler.getContainerConfig().setInitializationTimeout(
          INITIALIZATION_TIMEOUT_IN_MS);
      String[] springProfiles = listOfActiveSpringProfiles == null ? null
          : splitToArray(listOfActiveSpringProfiles);
      handler = new SpringBootProxyHandlerBuilder()
          .defaultProxy()
          .asyncInit(startTime)
          .springBootApplication(Application.class)
          .profiles(springProfiles)
          .buildAndInitialize();
    } catch (ContainerInitializationException e) {
      // if we fail here. We re-throw the exception to force another cold start
      throw new RuntimeException("Could not initialize Spring Boot application", e);
    }
  }

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream,
      Context context) throws IOException {
    handler.proxyStream(inputStream, outputStream, context);
  }
}
