package uk.gov.caz.psr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.CleanupDanglingPaymentsService;

@IntegrationTest
@Sql(scripts = "classpath:data/sql/dangling-payments-test-data.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class DanglingPaymentsCleanupTestIT {

  private static final int EXPECTED_NON_DANGLING_PAYMENTS_COUNT = 6;
  private static final int INITIAL_DANGLING_PAYMENTS_COUNT = 2;

  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private CleanupDanglingPaymentsService danglingPaymentsService;

  private ClientAndServer mockServer;

  @BeforeEach
  public void startMockServer() {
    mockServer = startClientAndServer(1080);
  }

  @AfterEach
  public void stopMockServer() {
    mockServer.stop();
  }

  @Test
  public void danglingPaymentsCleanupTest() {
    givenDanglingPaymentsWithExternalIds(
        "cancelled-payment-id",
        "expired-payment-id"
    );
    andNonDanglingPaymentsCountIs(EXPECTED_NON_DANGLING_PAYMENTS_COUNT);

    whenDanglingPaymentServiceIsCalled();

    thenStatusOf("cancelled-payment-id")
        .isFailed();
    thenStatusOf("expired-payment-id")
        .isFailed();
    andNonDanglingPaymentsCountIs(EXPECTED_NON_DANGLING_PAYMENTS_COUNT
        + INITIAL_DANGLING_PAYMENTS_COUNT);
    andDanglingPaymentsCountIsZero();
  }

  private void andDanglingPaymentsCountIsZero() {
    List<Payment> danglingPayments = paymentRepository.findDanglingPayments();
    assertThat(danglingPayments).isEmpty();
  }

  private void andNonDanglingPaymentsCountIs(int nonDanglingPaymentsCount) {
    int paymentsCount = nonDanglingPaymentsCount();
    assertThat(paymentsCount).isEqualTo(nonDanglingPaymentsCount);
  }

  private int nonDanglingPaymentsCount() {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
        "payment_provider_id IS NULL "
            + "OR payment_submitted_timestamp + INTERVAL '90 minutes' >= NOW() "
            + "OR payment_provider_status IN ('SUCCESS', 'FAILED', 'CANCELLED', 'ERROR')");
  }

  private DanglingPaymentAssertion thenStatusOf(String externalId) {
    return new DanglingPaymentAssertion(jdbcTemplate, externalId);
  }

  private void whenDanglingPaymentServiceIsCalled() {
    danglingPaymentsService.updateStatusesOfDanglingPayments();
  }

  private void givenDanglingPaymentsWithExternalIds(String cancelledPaymentExternalId,
      String expiredPaymentExternalId) {
    mockCancelledPaymentResponse(cancelledPaymentExternalId);
    mockExpiredPaymentResponse(expiredPaymentExternalId);
  }

  private void mockCancelledPaymentResponse(String externalId) {
    mockExternalPaymentResponse("cancelled", externalId);
  }

  private void mockExpiredPaymentResponse(String externalId) {
    mockExternalPaymentResponse("expired", externalId);
  }

  private void mockExternalPaymentResponse(String prefix, String externalId) {
    mockServer.when(
        HttpRequest.request()
            .withMethod("GET")
            .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .withPath("/v1/payments/" + externalId)
    ).respond(
        HttpResponse.response()
            .withStatusCode(HttpStatus.OK.value())
            .withHeader("Content-type", MediaType.APPLICATION_JSON.toString())
            .withBody(readFile(prefix + "-payment.json"))
    );
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(
        Resources.getResource("data/external/dangling/" + filename),
        Charsets.UTF_8
    );
  }

  @AllArgsConstructor
  @Value
  static class DanglingPaymentAssertion {
    JdbcTemplate jdbcTemplate;
    String externalId;

    public void isFailed() {
      int paymentsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "payment",
          "payment_provider_id = '" + externalId + "' "
              + "AND payment_provider_status = '" + ExternalPaymentStatus.FAILED.name() + "'");
      assertThat(paymentsCount).isEqualTo(1);
    }
  }
}
