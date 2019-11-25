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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.springframework.util.StreamUtils;

import uk.gov.caz.psr.Application;

@Slf4j
public class StreamLambdaHandler implements RequestStreamHandler {

  private static final String KEEP_WARM_ACTION = "keep-warm";
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
      throw new IllegalStateException("Could not initialize Spring Boot application", e);
    }
  }

  @Override
  public void handleRequest(final InputStream inputStream, final OutputStream outputStream,
      final Context context)
      throws IOException {
    String input = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
    log.info("Input received: " + input);
    if (isWarmupRequest(input)) {
      delayToAllowAnotherLambdaInstanceWarming();
      try (Writer osw = new OutputStreamWriter(outputStream)) {
        osw.write(LambdaContainerStats.getStats());
      }
    } else {
      LambdaContainerStats.setLatestRequestTime(LocalDateTime.now());
      handler.proxyStream(new ByteArrayInputStream(input.getBytes()), outputStream, context);
    }
  }

  /**
   * Delay lambda response if the container is new and cold to allow subsequent
   * keep-warm requests to be routed to a different lambda container.
   * 
   * @throws IOException when it is impossible to pause the thread
   */
  private void delayToAllowAnotherLambdaInstanceWarming() throws IOException {
    try {
      if (LambdaContainerStats.getLatestRequestTime() == null) {
        int sleepDuration = Integer
            .parseInt(Optional.ofNullable(
                System.getenv("thundra_lambda_warmup_warmupSleepDuration"))
                .orElse("100"));
        log.info(String.format("Container %s go to sleep for %f seconds",
                LambdaContainerStats.getInstanceId(),
                (double) sleepDuration / 1000));
        Thread.sleep(sleepDuration);
      }
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Determine if the incoming request is a keep-warm one.
   * 
   * @param action the request under examination
   * @return true if the incoming request is a keep-warm one otherwise false.
   */
  private boolean isWarmupRequest(final String action) {
    boolean isWarmupRequest = action.contains(KEEP_WARM_ACTION);
    if (isWarmupRequest) {
      log.info("Received lambda warmup request");
    }
    return isWarmupRequest;
  }

  /**
   * Contain information about the lambda container.
   */
  static class LambdaContainerStats {
    private static final String INSTANCE_ID = UUID.randomUUID().toString();
    private static final DateTimeFormatter formatter = 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static LocalDateTime latestRequestTime;

    private LambdaContainerStats() {
      throw new IllegalStateException("Statistic class");
    }

    /**
     * Get container instanceId.
     */
    public static String getInstanceId() {
      return INSTANCE_ID;
    }

    /**
     * Get the container stats.
     * 
     * @return a string that contains lambda container Id and (optionally) the time
     *         that the container last serve a request.
     */
    public static String getStats() {
      try {
        ObjectMapper obj = new ObjectMapper();
        Map<String, String> retVal = new HashMap<>();
        retVal.put("instanceId", INSTANCE_ID);
        if (latestRequestTime != null) {
          retVal.put("latestRequestTime",latestRequestTime.format(formatter));
        }
        return obj.writeValueAsString(retVal);
      } catch (JsonProcessingException ex) {
        return String.format("\"instanceId\": \"%s\"", INSTANCE_ID);
      }
    }

    /**
    * Get the time that the container last served a request.
    */
    public static LocalDateTime getLatestRequestTime() {
      return latestRequestTime;
    }

    /**
    * Set the time that the container serves a request.
    */
    public static void setLatestRequestTime(final LocalDateTime latestRequestTime) {
      LambdaContainerStats.latestRequestTime = latestRequestTime;
    }
  }
}