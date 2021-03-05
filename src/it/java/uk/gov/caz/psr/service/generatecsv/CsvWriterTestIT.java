package uk.gov.caz.psr.service.generatecsv;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.util.ResourceUtils;
import uk.gov.caz.psr.ExternalCallsIT;
import uk.gov.caz.psr.annotation.IntegrationTest;

@IntegrationTest
class CsvWriterTestIT extends ExternalCallsIT {

  @Autowired
  private CsvWriter csvWriter;

  @Test
  @Sql(scripts = {"classpath:data/sql/clear-all-payments.sql",
      "classpath:data/sql/csv-export/test-data.sql"},
      executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
      executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void shouldCreateWriterWithCsvContent() throws IOException {
    // given
    UUID accountId = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67a");
    UUID accountUserId = null;
    mockVccsCleanAirZonesCall();
    mockAccountServiceGetAllUsersCall(accountId.toString(), 200);

    // when
    Writer writer = csvWriter.createWriterWithCsvContent(accountId, accountUserId);

    // then
    assertThat(writer.toString()).isEqualTo(readExpectedCsv());
  }

  @SneakyThrows
  private static String readExpectedCsv() {
    String template = new String(Files.readAllBytes(
        ResourceUtils.getFile("classpath:data/csv/export/expected-payments-for-account.csv")
            .toPath()));
    return template.replace("{modificationDate}", LocalDate.now().toString());
  }
}