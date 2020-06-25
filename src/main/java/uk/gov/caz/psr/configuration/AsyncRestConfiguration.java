package uk.gov.caz.psr.configuration;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.correlationid.MdcCorrelationIdInjector;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.repository.WhitelistRepository;


/**
 * Configuration to setup retrofit2 services.
 */
@Configuration
@Slf4j
public class AsyncRestConfiguration {

  private static final String SLASH = "/";

  /**
   * VccsRepository spring bean.
   *
   * @return {@link VccsRepository}
   */
  @Bean
  public VccsRepository vccsRepository(ObjectMapper objectMapper,
      @Value("${services.vehicle-compliance-checker.root-url}") String vccsApiEndpoint,
      @Value("${services.read-timeout-seconds}") Integer readTimeoutSeconds,
      @Value("${services.connection-timeout-seconds}") Integer connectTimeoutSeconds) {
    return buildRetrofit(objectMapper, vccsApiEndpoint, readTimeoutSeconds, connectTimeoutSeconds)
        .create(VccsRepository.class);
  }

  /**
   * VccsRepository spring bean.
   *
   * @return {@link VccsRepository}
   */
  @Bean
  public AccountsRepository accountsRepository(ObjectMapper objectMapper,
      @Value("${services.accounts.root-url}") String accountsApiEndpoint,
      @Value("${services.read-timeout-seconds}") Integer readTimeoutSeconds,
      @Value("${services.connection-timeout-seconds}") Integer connectTimeoutSeconds) {
    return buildRetrofit(objectMapper, accountsApiEndpoint, readTimeoutSeconds,
        connectTimeoutSeconds)
        .create(AccountsRepository.class);
  }

  /**
   * Exposes {@link WhitelistRepository} as a spring bean.
   */
  @Bean
  public WhitelistRepository whitelistRepository(ObjectMapper objectMapper,
      @Value("${services.whitelist.root-url}") String whitelistRootUrl,
      @Value("${services.read-timeout-seconds}") Integer readTimeoutSeconds,
      @Value("${services.connection-timeout-seconds}") Integer connectTimeoutSeconds) {
    return buildRetrofit(objectMapper, whitelistRootUrl, readTimeoutSeconds, connectTimeoutSeconds)
        .create(WhitelistRepository.class);
  }

  /**
   * Creates {@link Retrofit} based on the passed params with the Jackson converter.
   */
  private Retrofit buildRetrofit(ObjectMapper objectMapper, String rootUrl,
      int readTimeoutSeconds, int connectTimeoutSeconds) {
    return new Retrofit.Builder()
        .baseUrl(requireNonNull(HttpUrl.parse(formatUrl(rootUrl))))
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .client(buildHttpClient(readTimeoutSeconds, connectTimeoutSeconds))
        .build();
  }

  /**
   * Helper method to build HttpClient fot the request.
   *
   * @return OkHttpClient with all needed attributes.
   */
  private OkHttpClient buildHttpClient(int readTimeoutInSeconds, int connectTimeoutInSeconds) {
    return new Builder()
        .readTimeout(readTimeoutInSeconds, TimeUnit.SECONDS)
        .connectTimeout(connectTimeoutInSeconds, TimeUnit.SECONDS)
        .addInterceptor(chain -> {
          Request original = chain.request();
          Request withCorrelationIdHeader = original.newBuilder()
              .header(Constants.X_CORRELATION_ID_HEADER, getOrGenerateCorrelationId(
                  MdcCorrelationIdInjector.getCurrentValue()))
              .build();
          return chain.proceed(withCorrelationIdHeader);
        })
        .build();
  }

  /**
   * Helper method to get current or generate new CorrelationId.
   *
   * @param correlationIdFromRequest correlation id from the request (can be null if the logic
   *     is invoked not in the HTTP request context)
   * @return correlationID as a String
   */
  private String getOrGenerateCorrelationId(String correlationIdFromRequest) {
    return correlationIdFromRequest == null
        ? UUID.randomUUID().toString()
        : correlationIdFromRequest;
  }

  /**
   * Helper method to format url.
   *
   * @param url to format
   * @return url ended with slash
   */
  private static String formatUrl(String url) {
    return url.endsWith(SLASH) ? url : url + SLASH;
  }

}
