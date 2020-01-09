package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit.jupiter.DisabledIf;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.model.Payment;

@Disabled("Because of ERD updates")
@IntegrationTest
class PaymentRepositoryTestIT {

  @Autowired
  private PaymentRepository paymentRepository;

  @Sql(scripts = {"classpath:data/sql/clear-all-payments.sql",
      "classpath:data/sql/add-payment-with-null-external-id.sql"},
      executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = {"classpath:data/sql/clear-all-payments.sql"},
      executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  @Test
  public void shouldFetchByIdPaymentWithNullExternalId() {
    // given
    UUID id = UUID.fromString("1883736c-016f-11ea-999f-974122a6ca41");

    // when
    Optional<Payment> payment = paymentRepository.findById(id);

    // then
    assertThat(payment).isNotEmpty();
  }
}