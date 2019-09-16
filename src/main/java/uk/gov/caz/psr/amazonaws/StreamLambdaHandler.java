package uk.gov.caz.psr.amazonaws;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import uk.gov.caz.psr.Application;

public class StreamLambdaHandler implements RequestStreamHandler {

  private static final Splitter SPLITTER = Splitter.on(',')
      .trimResults()
      .omitEmptyStrings();

  private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  static {
    try {
      String listOfActiveSpringProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
      if (listOfActiveSpringProfiles != null) {
        handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(Application.class,
            splitToArray(listOfActiveSpringProfiles));
      } else {
        handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(Application.class);
      }
    } catch (ContainerInitializationException e) {
      // if we fail here. We re-throw the exception to force another cold start
      e.printStackTrace();
      throw new RuntimeException("Could not initialize Spring Boot application", e);
    }
  }

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    handler.proxyStream(inputStream, outputStream, context);
  }

  private static String[] splitToArray(String activeProfiles) {
    return SPLITTER.splitToList(activeProfiles).toArray(new String[0]);
  }
}
