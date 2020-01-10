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
import uk.gov.caz.psr.model.CazEntrantPayment;
import uk.gov.caz.psr.model.InternalPaymentStatus;

@IntegrationTest
public class CazEntrantPaymentRespositoryTestIT {

  @Autowired
  private CazEntrantPaymentRepository cazEntrantPaymentRepository;

  @Sql(scripts = {"classpath:data/sql/clear-all-caz-entrant-payments.sql"},
      executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = {"classpath:data/sql/clear-all-caz-entrant-payments.sql"},
      executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  @Test
  public void shouldFetchByIdPaymentWithNullExternalId() {
    // given
    CazEntrantPayment cazEntrantPayment = CazEntrantPayment.builder()
        .cleanAirZoneId(UUID.randomUUID())
        .internalPaymentStatus(InternalPaymentStatus.PAID)
        .charge(100)
        .travelDate(LocalDate.now())
        .vrn("VRN")
        .tariffCode("TARIFF_CODE")
        .caseReference("Case Reference")
        .vehicleEntrantCaptured(true)
        .updateActor("API")
        .build();

    // when
    List<CazEntrantPayment> addedCazEntrantPayments = cazEntrantPaymentRepository.insert(Arrays.asList(cazEntrantPayment));

    // then
    CazEntrantPayment addedCazEntrantPayment = addedCazEntrantPayments.iterator().next();
    assertThat(addedCazEntrantPayment.getCleanAirZoneEntrantPaymentId()).isNotNull();
    assertThat(addedCazEntrantPayment.getTariffCode()).isEqualTo(cazEntrantPayment.getTariffCode());
  }
}
