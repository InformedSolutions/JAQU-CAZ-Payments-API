package uk.gov.caz.psr.service.generatecsv;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import uk.gov.caz.psr.ExternalCallsIT;
import uk.gov.caz.psr.annotation.IntegrationTest;

@IntegrationTest
class CsvContentGeneratorTestIT extends ExternalCallsIT {

  @Autowired
  private CsvContentGenerator csvContentGenerator;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @AfterEach
  public void clearDatabase() {
    executeSqlFrom("data/sql/clear-all-payments.sql");
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  @Nested
  class ModifiedByLocalAuthorities {

    @Test
    public void shouldGetAllCsvRowsWithAdditionalDataInHeader() {
      // given
      clearDatabase();
      executeSqlFrom("data/sql/csv-export/test-data.sql");

      UUID accountId = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67a");
      List<UUID> accountUserIds = Arrays.asList(
          UUID.fromString("ab3e9f4b-4076-4154-b6dd-97c5d4800b47"),
          UUID.fromString("3f319922-71d2-432c-9757-8e5f060c2447"),
          UUID.fromString("88732cca-a5c7-4ad6-a60d-7edede935915"));
      mockVccsCleanAirZonesCall();
      mockAccountServiceGetAllUsersCall(accountId.toString(), 200);

      // when
      List<String[]> csvRowResults = csvContentGenerator.generateCsvRows(accountId, accountUserIds);

      // then
      assertThat(csvRowResults).hasSize(8);
      assertThat(String.join(",", csvRowResults.get(0))).isEqualTo(
          "Date of payment,Payment made by,Clean Air Zone,Number plate,Dates paid for,Charge,"
              + "Payment reference,GOV.UK payment ID,Days paid for,Total amount paid,"
              + "Status,Date received from local authority,Case reference");
      assertThat(String.join(",", csvRowResults.get(1))).isEqualTo(
          "2019-11-25,Jan Kowalski,Birmingham,RD84VSX,2019-11-06,£28.00,1881,ext-payment-id-3,1,"
              + "£28.00,,,");
      assertThat(String.join(",", csvRowResults.get(2))).isEqualTo(
          "2019-11-24,Deleted user,Birmingham,PD84VSX,2019-11-04,£11.00,998,ext-payment-id-2,2,"
              + "£37.00,CHARGEBACK," + LocalDate.now().toString() + ",");
      assertThat(String.join(",", csvRowResults.get(3))).isEqualTo(
          "2019-11-24,Deleted user,Birmingham,QD84VSX,2019-11-05,£26.00,998,ext-payment-id-2,2,"
              + "£37.00,,,");
      // returns last LA modification details when more than one:
      assertThat(String.join(",", csvRowResults.get(4))).isEqualTo(
          "2019-11-23,Administrator,Birmingham,ND84VSX,2019-11-01,£8.00,87,ext-payment-id-1,4,£35.00,"
              + "REFUNDED," + LocalDate.now().toString() + ",");
      assertThat(String.join(",", csvRowResults.get(5))).isEqualTo(
          "2019-11-23,Administrator,Birmingham,ND84VSX,2019-11-02,£8.00,87,ext-payment-id-1,4,£35.00,"
              + "CHARGEBACK," + LocalDate.now().toString() + ",");
      assertThat(String.join(",", csvRowResults.get(6))).isEqualTo(
          "2019-11-23,Administrator,Birmingham,OD84VSX,2019-11-03,£8.00,87,ext-payment-id-1,4,"
              + "£35.00,,,");
      assertThat(String.join(",", csvRowResults.get(7))).isEqualTo(
          "2019-11-23,Administrator,Birmingham,PD84VSX,2019-11-04,£11.00,87,ext-payment-id-1,4,"
              + "£35.00,,,");
    }
  }

  @Nested
  class BasicCsvGeneration {

    @Test
    public void shouldGetCsvRowsWithoutAdditionalDataInHeader() {
      // given
      clearDatabase();
      executeSqlFrom("data/sql/csv-export/test-data-without-la-updates.sql");

      UUID accountId = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67a");
      List<UUID> accountUserIds = Arrays.asList(
          UUID.fromString("ab3e9f4b-4076-4154-b6dd-97c5d4800b47"),
          UUID.fromString("3f319922-71d2-432c-9757-8e5f060c2447"),
          UUID.fromString("88732cca-a5c7-4ad6-a60d-7edede935915"));
      mockVccsCleanAirZonesCall();
      mockAccountServiceGetAllUsersCall(accountId.toString(), 200);

      // when
      List<String[]> csvRowResults = csvContentGenerator.generateCsvRows(accountId, accountUserIds);

      // then
      assertThat(String.join(",", csvRowResults.get(0))).isEqualTo(
          "Date of payment,Payment made by,Clean Air Zone,Number plate,Dates paid for,Charge,"
              + "Payment reference,GOV.UK payment ID,Days paid for,Total amount paid");
    }
  }
}