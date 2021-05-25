package uk.gov.caz.psr.service.generatecsv;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.generatecsv.EnrichedCsvEntrantPayment;
import uk.gov.caz.psr.util.CurrencyFormatter;

@ExtendWith(MockitoExtension.class)
class CsvContentGeneratorStrategyFactoryTest {

  @Mock
  private CurrencyFormatter currencyFormatter;

  @InjectMocks
  private CsvContentGeneratorStrategyFactory csvContentGeneratorStrategyFactory;

  @Nested
  class EnrichedEntrantPaymentsWithNullStatuses {

    @Test
    public void returnsBaseCsvContentGeneratorStrategy() {
      // given
      List<EnrichedCsvEntrantPayment> data = mockEnrichedEntrantPayments(null);

      // when
      CsvContentGeneratorStrategy strategy = csvContentGeneratorStrategyFactory
          .createStrategy(data);

      // then
      assertThat(strategy).isInstanceOf(BaseCsvContentGeneratorStrategy.class);
    }
  }

  @Nested
  class EnrichedEntrantPaymentsWithNonLocalAuthorityStatuses {

    @Test
    public void returnsBaseCsvContentGeneratorStrategy() {
      // given
      List<EnrichedCsvEntrantPayment> data = mockEnrichedEntrantPayments("PAID");

      // when
      CsvContentGeneratorStrategy strategy = csvContentGeneratorStrategyFactory
          .createStrategy(data);

      // then
      assertThat(strategy).isInstanceOf(BaseCsvContentGeneratorStrategy.class);
    }
  }

  @Nested
  class EnrichedEntrantPaymentsWithLocalAuthorityStatuses {

    @ParameterizedTest
    @ValueSource(strings = {"REFUNDED", "CHARGEBACK"})
    public void returnsLocalAuthorityCsvContentGeneratorStrategy(String status) {
      // given
      List<EnrichedCsvEntrantPayment> data = mockEnrichedEntrantPayments(status);

      // when
      CsvContentGeneratorStrategy strategy = csvContentGeneratorStrategyFactory
          .createStrategy(data);

      // then
      assertThat(strategy).isInstanceOf(LocalAuthorityCsvContentGeneratorStrategy.class);
    }
  }

  private List<EnrichedCsvEntrantPayment> mockEnrichedEntrantPayments(String status) {
    return Collections.singletonList(
        EnrichedCsvEntrantPayment.builder()
            .paymentId(UUID.fromString("b8787d0f-fc49-4561-9804-f2d03a61815a"))
            .dateOfPayment(LocalDate.of(2020, 1, 1))
            .cazName("Birmingham")
            .vrn("CAS123")
            .charge(1000)
            .paymentMadeBy("Jan Kowalski")
            .dateReceivedFromLa(LocalDate.of(2020, 1, 1))
            .totalPaid(1000)
            .status(status)
            .caseReference("123abc")
            .build()
    );
  }
}