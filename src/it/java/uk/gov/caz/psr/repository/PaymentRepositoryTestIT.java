package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;

@Sql(scripts = {"classpath:data/sql/clear-all-payments.sql",
    "classpath:data/sql/add-caz-entrant-payments.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"classpath:data/sql/clear-all-payments.sql"},
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@IntegrationTest
class PaymentRepositoryTestIT {

  @Autowired
  private PaymentRepository paymentRepository;

  @Test
  public void shouldFetchByIdPayment() {
    // given
    UUID id = UUID.fromString("b71b72a5-902f-4a16-a91d-1a4463b801db");

    // when
    Optional<Payment> payment = paymentRepository.findById(id);

    // then
    assertThat(payment).isNotEmpty();
  }

  @Test
  public void shouldUpdatePaymentWithEntratntPaymentWhenExternalPayment() {
    // given
    UUID id = UUID.fromString("b71b72a5-902f-4a16-a91d-1a4463b801db");
    Payment payment = paymentRepository.findById(id).get();
    Payment paymentToUpdate = payment.toBuilder()
        .externalPaymentStatus(ExternalPaymentStatus.STARTED)
        .authorisedTimestamp(null)
        .externalId(UUID.randomUUID().toString())
        .build();

    // when
    paymentRepository.update(paymentToUpdate);
    Payment updatedPayment = paymentRepository.findById(id).get();

    // then
    assertThat(updatedPayment.getExternalPaymentStatus())
        .isNotEqualTo(payment.getExternalPaymentStatus());
    assertThat(updatedPayment.getExternalId()).isNotEqualTo(payment.getExternalId());
    assertThat(updatedPayment.getExternalPaymentStatus())
        .isEqualTo(paymentToUpdate.getExternalPaymentStatus());
    assertThat(updatedPayment.getExternalId()).isEqualTo(paymentToUpdate.getExternalId());
  }
}