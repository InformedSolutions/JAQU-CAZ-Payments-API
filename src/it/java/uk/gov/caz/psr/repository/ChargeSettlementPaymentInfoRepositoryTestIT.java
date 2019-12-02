package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.persistence.criteria.Join;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.PaymentInfo_;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo_;
import uk.gov.caz.psr.repository.jpa.PaymentInfoRepository;

@IntegrationTest
public class ChargeSettlementPaymentInfoRepositoryTestIT {

  private static final UUID ANY_PRESENT_CAZ_ID =
      UUID.fromString("53e03a28-0627-11ea-9511-ffaaee87e375");
  private static final UUID ANY_ABSENT_CAZ_ID =
      UUID.fromString("d83bf00e-1028-11ea-be9e-a3f00ff90b28");
  private static final String ANY_PRESENT_VRN = "ND84VSX";
  private static final String ANY_ABSENT_VRN = "AB84SEN";

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
  private PaymentInfoRepository paymentInfoRepository;

  @Nested
  class FindByCleanZoneIdAndVrnAndEntryDatesRange {

    @Test
    public void shouldReturnEmptyListForNotCoveredDateRange() {
      // given
      UUID caz = ANY_PRESENT_CAZ_ID;
      String vrn = ANY_PRESENT_VRN;
      LocalDate start = LocalDate.parse("2018-07-04");
      LocalDate end = LocalDate.parse("2019-10-08");

      // when
      List<PaymentInfo> result =
          paymentInfoRepository.findAll(byCazAndVrnAndDateRangeSpecification(caz, vrn, start, end));

      // then
      assertThat(result).isEmpty();
    }

    @Test
    public void shouldReturnEmptyListForNotCoveredVrn() {
      // given
      UUID caz = ANY_PRESENT_CAZ_ID;
      String vrn = ANY_ABSENT_VRN;
      LocalDate start = LocalDate.parse("2018-07-04");
      LocalDate end = LocalDate.parse("2019-11-07");

      // when
      List<PaymentInfo> result =
          paymentInfoRepository.findAll(byCazAndVrnAndDateRangeSpecification(caz, vrn, start, end));

      // then
      assertThat(result).isEmpty();
    }

    @Test
    public void shouldReturnEmptyListForNotCoveredCazId() {
      // given
      UUID caz = ANY_ABSENT_CAZ_ID;
      String vrn = ANY_PRESENT_VRN;
      LocalDate start = LocalDate.parse("2018-07-04");
      LocalDate end = LocalDate.parse("2019-11-07");

      // when
      List<PaymentInfo> result =
          paymentInfoRepository.findAll(byCazAndVrnAndDateRangeSpecification(caz, vrn, start, end));

      // then
      assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("uk.gov.caz.psr.repository.ChargeSettlementPaymentInfoRepositoryTestIT#partiallyCoveredDateRange")
    public void shouldReturnListForPartiallyCoveredDateRange(LocalDate start, LocalDate end) {
      // given
      UUID caz = ANY_PRESENT_CAZ_ID;
      String vrn = ANY_PRESENT_VRN;

      // when
      List<PaymentInfo> result =
          paymentInfoRepository.findAll(byCazAndVrnAndDateRangeSpecification(caz, vrn, start, end));

      // then
      assertThat(result).hasSize(1);
      assertThat(result)
          .allSatisfy(payment -> assertThat(payment.getVehicleEntrantPaymentInfoList()).allSatisfy(
              vehicleEntrantPayment -> assertThat(vehicleEntrantPayment.getCleanAirZoneId())
                  .isEqualTo(ANY_PRESENT_CAZ_ID)));
    }

    @Test
    public void shouldReturnRecordsForDifferentPaymentObjects() {
      // given
      UUID caz = ANY_PRESENT_CAZ_ID;
      String vrn = ANY_PRESENT_VRN;
      LocalDate start = LocalDate.parse("2018-10-01");
      LocalDate end = LocalDate.parse("2019-12-01");

      // when
      List<PaymentInfo> result =
          paymentInfoRepository.findAll(byCazAndVrnAndDateRangeSpecification(caz, vrn, start, end));

      // then
      assertThat(result).hasSize(2);
      assertThat(result).allSatisfy(payment -> assertThat(payment.getExternalPaymentStatus())
          .isIn(EnumSet.of(ExternalPaymentStatus.SUCCESS, ExternalPaymentStatus.FAILED)));
    }

    private Specification<PaymentInfo> byCazAndVrnAndDateRangeSpecification(UUID caz, String vrn,
        LocalDate start, LocalDate end) {
      return (root, criteriaQuery, criteriaBuilder) -> {
        criteriaQuery.distinct(true);
        Join<PaymentInfo, VehicleEntrantPaymentInfo> join =
            (Join) root.fetch(PaymentInfo_.vehicleEntrantPaymentInfoList);
        return criteriaBuilder.and(
            criteriaBuilder.and(
                criteriaBuilder.equal(join.get(VehicleEntrantPaymentInfo_.cleanAirZoneId), caz)),
            criteriaBuilder
                .and(criteriaBuilder.equal(join.get(VehicleEntrantPaymentInfo_.vrn), vrn)),
            criteriaBuilder.and(criteriaBuilder
                .greaterThanOrEqualTo(join.get(VehicleEntrantPaymentInfo_.travelDate), start)),
            criteriaBuilder.and(criteriaBuilder
                .lessThanOrEqualTo(join.get(VehicleEntrantPaymentInfo_.travelDate), end)));
      };
    }
  }

  @Nested
  class FindByCleanZoneIdAndEntryDatesRange {
    @Test
    public void shouldReturnRecordsForSpecifiedDate() {
      // given
      UUID caz = ANY_PRESENT_CAZ_ID;
      LocalDate start = LocalDate.parse("2019-11-02");
      LocalDate end = start;

      // when
      List<PaymentInfo> result =
          paymentInfoRepository.findAll(byCazAndDateRangeSpecification(caz, start, end));

      // then
      assertThat(result).hasSize(2);
      assertThat(result)
          .allSatisfy(payment -> assertThat(payment.getVehicleEntrantPaymentInfoList())
              .allSatisfy(vehicleEntrantPayment -> {
                assertThat(vehicleEntrantPayment.getVrn()).isIn("ND84VSX", "AB11CDE");
                assertThat(vehicleEntrantPayment.getCleanAirZoneId()).isEqualTo(ANY_PRESENT_CAZ_ID);
                assertThat(vehicleEntrantPayment.getTravelDate()).isEqualTo(start);
              }));
    }

    @Test
    public void shouldReturnRecordsForSpecifiedDateRange() {
      // given
      UUID caz = ANY_PRESENT_CAZ_ID;
      LocalDate start = LocalDate.parse("2019-11-02");
      LocalDate end = LocalDate.parse("2019-11-06");

      // when
      List<PaymentInfo> result =
          paymentInfoRepository.findAll(byCazAndDateRangeSpecification(caz, start, end));

      // then
      assertThat(result).hasSize(3);
      assertThat(result)
          .allSatisfy(payment -> assertThat(payment.getVehicleEntrantPaymentInfoList())
              .allSatisfy(vehicleEntrantPayment -> {
                assertThat(vehicleEntrantPayment.getVrn()).isIn("ND84VSX", "AB11CDE");
                assertThat(vehicleEntrantPayment.getCleanAirZoneId()).isEqualTo(ANY_PRESENT_CAZ_ID);
                assertThat(vehicleEntrantPayment.getTravelDate()).isBeforeOrEqualTo(end);
                assertThat(vehicleEntrantPayment.getTravelDate()).isAfterOrEqualTo(start);
              }));
    }

    private Specification<PaymentInfo> byCazAndDateRangeSpecification(UUID caz, LocalDate start,
        LocalDate end) {
      return (root, criteriaQuery, criteriaBuilder) -> {
        criteriaQuery.distinct(true);
        Join<PaymentInfo, VehicleEntrantPaymentInfo> join =
            (Join) root.fetch(PaymentInfo_.vehicleEntrantPaymentInfoList);
        return criteriaBuilder.and(
            criteriaBuilder.and(
                criteriaBuilder.equal(join.get(VehicleEntrantPaymentInfo_.cleanAirZoneId), caz)),
            criteriaBuilder.and(criteriaBuilder
                .greaterThanOrEqualTo(join.get(VehicleEntrantPaymentInfo_.travelDate), start)),
            criteriaBuilder.and(criteriaBuilder
                .lessThanOrEqualTo(join.get(VehicleEntrantPaymentInfo_.travelDate), end)));
      };
    }
  }

  @Nested
  class FindByCleanZoneIdAndVrn {
    @Test
    public void shouldReturnRecordsForPresentVrn() {
      // given
      UUID caz = ANY_PRESENT_CAZ_ID;
      String vrn = "AB11CDE";

      // when
      List<PaymentInfo> result = paymentInfoRepository.findAll(byCazAndVrnSpecification(caz, vrn));

      // then
      assertThat(result).hasSize(1);
      assertThat(result).allSatisfy(payment -> {
        assertThat(payment.getVehicleEntrantPaymentInfoList()).hasSize(2);
        assertThat(payment.getVehicleEntrantPaymentInfoList()).allSatisfy(vehicleEntrantPayment -> {
          assertThat(vehicleEntrantPayment.getCleanAirZoneId()).isEqualTo(ANY_PRESENT_CAZ_ID);
          assertThat(vehicleEntrantPayment.getVrn()).isEqualTo(vrn);
        });
      });
    }

    @Test
    public void shouldReturnEmptyListForPresentVrnAndAbsentCaz() {
      // given
      String vrn = "AB11CDE";
      UUID caz = ANY_ABSENT_CAZ_ID;

      // when
      List<PaymentInfo> result = paymentInfoRepository.findAll(byCazAndVrnSpecification(caz, vrn));

      // then
      assertThat(result).isEmpty();
    }

    private Specification<PaymentInfo> byCazAndVrnSpecification(UUID caz, String vrn) {
      return (root, criteriaQuery, criteriaBuilder) -> {
        criteriaQuery.distinct(true);
        Join<PaymentInfo, VehicleEntrantPaymentInfo> join =
            (Join) root.fetch(PaymentInfo_.vehicleEntrantPaymentInfoList);
        return criteriaBuilder.and(
            criteriaBuilder.and(
                criteriaBuilder.equal(join.get(VehicleEntrantPaymentInfo_.cleanAirZoneId), caz)),
            criteriaBuilder
                .and(criteriaBuilder.equal(join.get(VehicleEntrantPaymentInfo_.vrn), vrn)));
      };
    }

    @Test
    public void shouldReturnEmptyListForPresentCazAndAbsentVrn() {
      // given
      String vrn = ANY_ABSENT_VRN;
      UUID caz = ANY_PRESENT_CAZ_ID;

      // when
      List<PaymentInfo> result = paymentInfoRepository.findAll(byCazAndVrnSpecification(caz, vrn));

      // then
      assertThat(result).isEmpty();
    }
  }

  static Stream<Arguments> partiallyCoveredDateRange() {
    return Stream.of(Arguments.of(LocalDate.parse("2019-11-01"), LocalDate.parse("2019-11-01")), // inclusive
                                                                                                 // #1
        Arguments.of(LocalDate.parse("2019-11-07"), LocalDate.parse("2019-11-07")), // inclusive #2
        Arguments.of(LocalDate.parse("2019-11-07"), LocalDate.parse("2019-11-08")),
        Arguments.of(LocalDate.parse("2019-11-07"), LocalDate.parse("2020-04-08")));
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }
}
