package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.model.PaymentStatus;

@IntegrationTest
@Sql(scripts = "classpath:data/sql/add-payments-for-payment-status.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"classpath:data/sql/clear-all-payments.sql"},
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class PaymentStatusRepositoryTestIT {

  @Autowired
  private PaymentStatusRepository repository;

  @Test
  public void shouldReturnNonEmptyCollection() {
    // given
    LocalDate dateOfCazEntry = LocalDate.of(2019, 11, 4);
    String vrn = "ND84VSX";
    UUID cazId = UUID.fromString("b8e53786-c5ca-426a-a701-b14ee74857d4");

    // when
    Collection<PaymentStatus> results = repository
        .findByCazIdAndVrnAndEntryDate(cazId, vrn, dateOfCazEntry);

    // then
    assertThat(results).isNotEmpty();
  }

  @Test
  public void shouldReturnEmptyCollectionWhenNotSuccessfullyPaidAndNotEnteredCAZ() {
    // given
    LocalDate dateOfCazEntry = LocalDate.of(2019, 11, 6);
    String vrn = "ND84VSX";
    UUID cazId = UUID.fromString("b8e53786-c5ca-426a-a701-b14ee74857d4");

    // when
    Collection<PaymentStatus> results = repository
        .findByCazIdAndVrnAndEntryDate(cazId, vrn, dateOfCazEntry);

    // then
    assertThat(results).isEmpty();
  }

  @Test
  public void shouldReturnNonEmptyCollectionWhenMarkedByLAasPaidButEntrantNotCaptured() {
    // given
    LocalDate dateOfCazEntry = LocalDate.of(2019, 11, 7);
    String vrn = "ND84VSX";
    UUID cazId = UUID.fromString("b8e53786-c5ca-426a-a701-b14ee74857d4");

    // when
    Collection<PaymentStatus> results = repository
        .findByCazIdAndVrnAndEntryDate(cazId, vrn, dateOfCazEntry);

    // then
    assertThat(results).isNotEmpty();
  }

  @Test
  public void shouldReturnNonEmptyCollectionWhenRefundedByLaAndEntrantNotCaptured() {
    // given
    LocalDate dateOfCazEntry = LocalDate.of(2019, 11, 8);
    String vrn = "ND84VSX";
    UUID cazId = UUID.fromString("b8e53786-c5ca-426a-a701-b14ee74857d4");

    // when
    Collection<PaymentStatus> results = repository
        .findByCazIdAndVrnAndEntryDate(cazId, vrn, dateOfCazEntry);

    // then
    assertThat(results).isNotEmpty();
  }
}

