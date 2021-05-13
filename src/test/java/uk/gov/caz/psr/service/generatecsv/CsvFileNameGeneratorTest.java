package uk.gov.caz.psr.service.generatecsv;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CsvFileNameGeneratorTest {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
      .ofPattern("ddMMMMyyyy-HHmmss");

  private CsvFileNameGenerator csvFileNameGenerator = new CsvFileNameGenerator();

  @Test
  public void shouldGenerateFileName() {
    // when
    String fileName = csvFileNameGenerator.generate();

    // then
    assertThat(fileName)
        .contains("Payment-history-")
        .contains(DATE_TIME_FORMATTER.format(LocalDateTime.now()))
        .contains(".csv");
  }
}
