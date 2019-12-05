package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;

@IntegrationTest
public class ChargeSettlementPaymentInfoServiceTestIT {

  private static final UUID PRESENT_CAZ_ID =
      UUID.fromString("53e03a28-0627-11ea-9511-ffaaee87e375");
  private static final UUID ABSENT_CAZ_ID =
      UUID.fromString("d83bf00e-1028-11ea-be9e-a3f00ff90b28");
  private static final String NOT_EXISTING_VRN = "AB84SEN";
  private static final String NOT_EXISTING_EXTERNAL_ID = "ext-payment-id-not-exists";
  private static final String PAYMENT_2_EXTERNAL_ID = "ext-payment-id-2";
  private static final String PAYMENT_3_EXTERNAL_ID = "ext-payment-id-3";
  private static final String PAYMENT_3_VRN = "AB11CDE";
  private static final UUID P1_VP_1_ID = UUID.fromString("c59d0f46-0f8d-11ea-bbdd-9bfba959fef8");
  private static final UUID P1_VP2_ID = UUID.fromString("c9801856-0f8d-11ea-bbdd-0fb9b9867da0");
  private static final UUID P1_VP3_ID = UUID.fromString("ce083912-0f8d-11ea-bbdd-47debb103c06");
  private static final UUID P2_VP1_ID = UUID.fromString("62320a6c-0f90-11ea-bbdd-b3fa7794610e");
  private static final UUID P2_VP2_ID = UUID.fromString("65821f90-0f90-11ea-bbdd-9bba0c562c82");
  private static final UUID P3_VP1_ID = UUID.fromString("e218c724-102c-11ea-be9e-973e776167e1");
  private static final UUID P3_VP2_ID = UUID.fromString("e593130a-102c-11ea-be9e-975729b598b5");

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
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder().paymentProviderId(PAYMENT_2_EXTERNAL_ID).build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).hasSize(2);
      assertThat(result).extracting("id")
          .contains(
              P2_VP1_ID,
              P2_VP2_ID
          );
    }

    @Test
    public void shouldReturnEmptyListIfNotFound() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder().paymentProviderId(NOT_EXISTING_EXTERNAL_ID).build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  class FindByCleanZoneIdAndVrn {

    @Test
    public void shouldReturnExactMatch() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder().vrn(PAYMENT_3_VRN).build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).hasSize(2);
      assertThat(result).extracting("id")
          .contains(
              P3_VP1_ID,
              P3_VP2_ID
          );
    }

    @Test
    public void shouldReturnEmptyListIfNotFound() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder().vrn(NOT_EXISTING_VRN).build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  class FindByCleanZoneIdAndTravelDatesFromAndTo {

    @Test
    public void shouldFindTwoPayments() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder()
          .fromDatePaidFor(LocalDate.of(2019, 11, 1))
          .toDatePaidFor(LocalDate.of(2019, 11, 3))
          .build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).hasSize(5);
      assertThat(result).extracting("id")
          .contains(
              P1_VP_1_ID,
              P1_VP2_ID,
              P1_VP3_ID,
              P3_VP1_ID,
              P3_VP2_ID
          );
    }

    @Test
    public void shouldFindAllPayments() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder()
          .fromDatePaidFor(LocalDate.of(2019, 11, 1))
          .toDatePaidFor(LocalDate.of(2019, 11, 7))
          .build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).hasSize(9);
    }

    @Test
    public void shouldReturnEmptyListIfNotFound() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder()
          .fromDatePaidFor(LocalDate.of(2020, 11, 1))
          .toDatePaidFor(LocalDate.of(2020, 11, 7))
          .build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  class FindByCleanZoneIdAndTravelDateFromOnly {

    @Test
    public void shouldFindTwoPayments() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder()
          .fromDatePaidFor(LocalDate.of(2019, 11, 1))
          .build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).hasSize(2);
      assertThat(result).extracting("id")
          .contains(
              P1_VP_1_ID,
              P3_VP1_ID
          );
    }

    @Test
    public void shouldFindOnlyOnePayment() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder()
          .fromDatePaidFor(LocalDate.of(2019, 11, 7))
          .build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).hasSize(1);
      assertThat(result).extracting("id")
          .contains(P2_VP2_ID);
    }

    @Test
    public void shouldReturnEmptyListIfNotFound() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder()
          .fromDatePaidFor(LocalDate.of(2020, 11, 1))
          .build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  class FindByCleanZoneIdAndTravelDateToOnly {

    @Test
    public void shouldFindTwoPayments() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder()
          .toDatePaidFor(LocalDate.of(2019, 11, 2))
          .build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).hasSize(2);
      assertThat(result).extracting("id")
          .contains(
              P1_VP_1_ID,
              P3_VP1_ID
          );
    }

    @Test
    public void shouldFindOnlyOnePayment() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder()
          .toDatePaidFor(LocalDate.of(2019, 11, 8))
          .build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).hasSize(1);
      assertThat(result).extracting("id")
          .contains(P2_VP2_ID);
    }

    @Test
    public void shouldReturnEmptyListIfNotFound() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder()
          .toDatePaidFor(LocalDate.of(2020, 11, 7))
          .build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  class FindByAllParams {

    @Test
    public void shouldReturnExactMatch() {
      // given
      UUID caz = PRESENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder()
          .vrn(PAYMENT_3_VRN)
          .paymentProviderId(PAYMENT_3_EXTERNAL_ID)
          .fromDatePaidFor(LocalDate.of(2019, 11, 1))
          .toDatePaidFor(LocalDate.of(2019, 11, 2))
          .build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).hasSize(2);
      assertThat(result).extracting("id")
          .contains(
              P3_VP1_ID,
              P3_VP2_ID
          );
    }

    @Test
    public void shouldReturnEmptyListIfCazIdNotFound() {
      // given
      UUID caz = ABSENT_CAZ_ID;
      PaymentInfoRequest paymentInfoRequest = PaymentInfoRequest.builder()
          .vrn(PAYMENT_3_VRN)
          .paymentProviderId(PAYMENT_3_EXTERNAL_ID)
          .fromDatePaidFor(LocalDate.of(2019, 11, 1))
          .toDatePaidFor(LocalDate.of(2019, 11, 2))
          .build();

      // when
      List<VehicleEntrantPaymentInfo> result = paymentInfoService.findPaymentInfo(paymentInfoRequest, caz);

      // then
      assertThat(result).isEmpty();
    }
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }
}
