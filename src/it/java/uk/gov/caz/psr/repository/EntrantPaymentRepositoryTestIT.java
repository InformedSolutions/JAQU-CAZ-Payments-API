package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;

@Sql(scripts = {"classpath:data/sql/clear-all-payments.sql",
    "classpath:data/sql/add-caz-entrant-payments.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"classpath:data/sql/clear-all-payments.sql"},
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@IntegrationTest
public class EntrantPaymentRepositoryTestIT {

  @Autowired
  private EntrantPaymentRepository entrantPaymentRepository;

  @Test
  public void shouldFetchByIdPaymentWithNullExternalId() {
    // given
    EntrantPayment entrantPayment = EntrantPayment.builder()
        .cleanAirZoneId(UUID.randomUUID())
        .internalPaymentStatus(InternalPaymentStatus.PAID)
        .charge(100)
        .travelDate(LocalDate.now())
        .vrn("VRN")
        .tariffCode("TARIFF_CODE")
        .caseReference("Case Reference")
        .vehicleEntrantCaptured(true)
        .updateActor(EntrantPaymentUpdateActor.VCCS_API)
        .build();

    // when
    List<EntrantPayment> addedEntrantPayments = entrantPaymentRepository
        .insert(Arrays.asList(entrantPayment));

    // then
    EntrantPayment addedCazEntrantPayment = addedEntrantPayments.iterator().next();
    assertThat(addedCazEntrantPayment.getCleanAirZoneEntrantPaymentId()).isNotNull();
    assertThat(addedCazEntrantPayment.getTariffCode()).isEqualTo(entrantPayment.getTariffCode());
  }

  @Test
  public void shouldFetchByPaymentIdWhenDataPresentInDB() {
    // given
    UUID paymentId = UUID.fromString("dabc1391-ff31-427a-8000-69037deb2d3a");

    // when
    List<EntrantPayment> foundCazEntrantPayments = entrantPaymentRepository
        .findByPaymentId(paymentId);

    // then
    assertThat(foundCazEntrantPayments).isNotEmpty();
  }

  @Test
  public void shouldNotFetchByPaymentIdWhenDataNotPresentInDB() {
    // given
    UUID paymentId = UUID.randomUUID();

    // when
    List<EntrantPayment> foundCazEntrantPayments = entrantPaymentRepository
        .findByPaymentId(paymentId);

    // then
    assertThat(foundCazEntrantPayments).isEmpty();
  }
}
