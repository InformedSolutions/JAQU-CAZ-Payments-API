package uk.gov.caz.psr.service.generatecsv;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.generatecsv.EnrichedCsvEntrantPayment;
import uk.gov.caz.psr.util.CurrencyFormatter;

@ExtendWith(MockitoExtension.class)
class BaseCsvContentGeneratorStrategyTest {

  CurrencyFormatter currencyFormatter = new CurrencyFormatter();

  CsvContentGeneratorStrategy csvContentGeneratorStrategy =
      new BaseCsvContentGeneratorStrategy(currencyFormatter);

  @Nested
  class GenerateCsvHeader {

    @Test
    public void shouldGenerateHeaderWithoutAnyAdditionalData() {
      // when
      String[] header = csvContentGeneratorStrategy.generateCsvHeader();

      // then
      assertThat(String.join(",", header)).isEqualTo(
          "Date of payment,Payment made by,"
              + "Clean Air Zone,Number plate,Dates paid for,Charge,Payment reference,"
              + "GOV.UK payment ID,Days paid for,Total amount paid");
    }
  }

  @Nested
  class GenerateCsvContent {

    @Test
    public void shouldGenerateCsvBodyWithoutAnyAdditionalData() {
      // given
      List<EnrichedCsvEntrantPayment> enrichedCsvEntrantPayments = mockEnrichedEntrantPayments();

      // when
      List<String[]> csvContent = csvContentGeneratorStrategy
          .generateCsvContent(enrichedCsvEntrantPayments);

      // then
      assertThat(String.join(",", csvContent.get(0)))
          .isEqualTo("2020-01-01,Jan Kowalski,Birmingham,CAS123,,£10.00,,,,£10.00");
    }

    private List<EnrichedCsvEntrantPayment> mockEnrichedEntrantPayments() {
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
              .status("REFUNDED")
              .caseReference("123abc")
              .build()
      );
    }
  }
}