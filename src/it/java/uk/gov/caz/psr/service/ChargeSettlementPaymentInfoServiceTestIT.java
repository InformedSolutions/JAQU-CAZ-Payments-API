package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.info.PaymentInfo;

@IntegrationTest
public class ChargeSettlementPaymentInfoServiceTestIT {

  private static final UUID PRESENT_CAZ_ID =
      UUID.fromString("53e03a28-0627-11ea-9511-ffaaee87e375");
  private static final UUID ABSENT_CAZ_ID =
      UUID.fromString("d83bf00e-1028-11ea-be9e-a3f00ff90b28");
  private static final String ANY_ABSENT_VRN = "AB84SEN";
  private static final String PAYMENT_1_EXTERNAL_ID = "ext-payment-id-1";
  private static final UUID PAYMENT_1_ID = UUID.fromString("d80deb4e-0f8a-11ea-8dc9-93fa5be4476e");
  private static final String PAYMENT_1_VRN = "ND84VSX";
  private static final String PAYMENT_2_EXTERNAL_ID = "ext-payment-id-2";
  private static final String PAYMENT_2_VRN = "ND84VSX";
  private static final String PAYMENT_3_EXTERNAL_ID = "ext-payment-id-3";
  private static final String PAYMENT_3_VRN = "AB11CDE";

  @Autowired
  private DataSource dataSource;

  @BeforeEach
  public void insertTestData() {
    // we cannot use SQL annotations on this class, see:
    // https://github.com/spring-projects/spring-framework/issues/19930
    executeSqlFrom("data/sql/charge-settlement/payment-info/test-data.sql");
  }

  @AfterEach
  public void clearDatabase() {
    executeSqlFrom("data/sql/clear-all-payments.sql");
  }

  @Autowired
  private ChargeSettlementPaymentInfoService paymentInfoService;

  @Nested
  class FindByCleanZoneIdAndPaymentId {

    @Test
    public void shouldReturnExactMatch() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = new PaymentInfoRequest(
          PAYMENT_1_EXTERNAL_ID,
          null,
          null,
          null
      );

      // when
      List<PaymentInfo> result = paymentInfoService.filter(paymentInfoRequest, caz);

      // then
      assertThat(result).hasSize(1);
      assertThat(result).extracting("id")
          .contains(PAYMENT_1_ID);
    }
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }
}
