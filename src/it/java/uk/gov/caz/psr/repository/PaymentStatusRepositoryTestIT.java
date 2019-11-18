package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.model.PaymentStatus;

@IntegrationTest
class PaymentStatusRepositoryTestIT {

  @Autowired
  private PaymentStatusRepository repository;

  @Sql(scripts = {"classpath:data/sql/add-payments.sql"},
      executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = {"classpath:data/sql/clear-all-payments.sql"},
      executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  @Test
  public void shouldReturnNonEmptyCollection() {
    // given
    LocalDate dateOfCazEntry = LocalDate.of(2019, 11, 04);
    String vrn = "ND84VSX";
    UUID cazId = UUID.fromString("b8e53786-c5ca-426a-a701-b14ee74857d4");

    // when
    Collection<PaymentStatus> results = repository
        .findByCazIdAndVrnAndEntryDate(cazId, vrn, dateOfCazEntry);

    // then
    assertThat(results).isNotEmpty();
  }
}

