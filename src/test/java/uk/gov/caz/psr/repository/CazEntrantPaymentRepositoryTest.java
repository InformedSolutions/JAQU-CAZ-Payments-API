package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.psr.model.CazEntrantPayment;
import uk.gov.caz.psr.model.VehicleEntrant;

@ExtendWith(MockitoExtension.class)
class CazEntrantPaymentRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private CazEntrantPaymentRepository cazEntrantPaymentRepository;

  @Nested
  class Insert {

    @Test
    public void shouldThrowNullPointerExceptionWhenInsertListIsNull() {
      // given
      List<CazEntrantPayment> input = null;

      // when
      Throwable throwable = catchThrowable(() -> cazEntrantPaymentRepository.insert(input));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("CAZ entrant payments cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenInsertListIsEmpty() {
      // given
      List<CazEntrantPayment> input = Collections.emptyList();

      // when
      Throwable throwable = catchThrowable(() -> cazEntrantPaymentRepository.insert(input));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("CAZ entrant payments cannot be empty");
    }

    @Nested
    class UpdateList {

      @Test
      public void shouldThrowNullPointerExceptionWhenListIsNull() {
        // given
        List<CazEntrantPayment> input = null;

        // when
        Throwable throwable = catchThrowable(() -> cazEntrantPaymentRepository.update(input));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class)
            .hasMessage("CAZ entrant payments cannot be null");
      }
    }

    @Nested
    class Update {

      @Test
      public void shouldThrowNullPointerExceptionWhenCazEntrantPaymentIsNull() {
        // given
        CazEntrantPayment cazEntrantPayment = null;

        // when
        Throwable throwable = catchThrowable(
            () -> cazEntrantPaymentRepository.update(cazEntrantPayment));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class)
            .hasMessage("CAZ entrant payments cannot be null");
      }
    }

    @Nested
    class FindSuccessfullyPaid {

      @Test
      public void shouldThrowNullPointerExceptionWhenCazEntrantIsNull() {
        // given
        VehicleEntrant vehicleEntrant = null;

        // when
        Throwable throwable = catchThrowable(
            () -> cazEntrantPaymentRepository.findSuccessfullyPaid(vehicleEntrant));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class)
            .hasMessage("Vehicle Entrant cannot be null");
      }
    }

    @Nested
    class FindOneByVrnAndCazEntryDate {

      @Test
      public void shouldThrowNullPointerExceptionWhenCleanZoneIdIsNull() {
        // given
        UUID cleanZoneId = null;

        // when
        Throwable throwable = catchThrowable(
            () -> cazEntrantPaymentRepository
                .findOneByVrnAndCazEntryDate(cleanZoneId, "VRN123", LocalDate.now()));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class)
            .hasMessage("cleanZoneId cannot be null");
      }

      @Test
      public void shouldThrowNullPointerExceptionWhenCazEntryDateIsNull() {
        // given
        LocalDate cazEntryDate = null;

        // when
        Throwable throwable = catchThrowable(
            () -> cazEntrantPaymentRepository
                .findOneByVrnAndCazEntryDate(UUID.randomUUID(), "VRN123", cazEntryDate));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class)
            .hasMessage("cazEntryDate cannot be null");
      }

      @Test
      public void shouldThrowNIllegalArgumentExceptionWhenVrnIsEmpty() {
        // given
        String vrn = null;

        // when
        Throwable throwable = catchThrowable(
            () -> cazEntrantPaymentRepository
                .findOneByVrnAndCazEntryDate(UUID.randomUUID(), "", LocalDate.now()));

        // then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("VRN cannot be empty");
      }
//    @Test
//    public void shouldThrowNIllegalArgumentExceptionWhenVrnIsEmpty() {
//      // given
//      String vrn = null;
//
//      // when
//      Throwable throwable = catchThrowable(
//          () -> cazEntrantPaymentRepository
//              .findOneByVrnAndCazEntryDate(UUID.randomUUID(), "", LocalDate.now()));
//
//      // then
//      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
//          .hasMessage("VRN cannot be empty");
//    }

//    TODO: Fix with the payment updates CAZ-1716
//    @Test
//    public void shouldThrowNIllegalStateExceptionWhenFoundMoreThanOneVehicleEntrantPayment() {
//      // given
//      mockMultipleItemsFoundInDatabaseForFindOneWithVrn();
//
//      // when
//      Throwable throwable = catchThrowable(
//          () -> vehicleEntrantPaymentRepository
//              .findOnePaidByVrnAndCazEntryDate(UUID.randomUUID(), "VRN", LocalDate.now()));
//
//      // then
//      assertThat(throwable).isInstanceOf(NotUniqueVehicleEntrantPaymentFoundException.class)
//          .hasMessage("Not able to find unique VehicleEntrantPayment");
//    }
//
//    @Test
//    public void shouldReturnOptionalEmptyWhenNoVehicleEntrantPaymentFound() {
//      // given
//      mockNoItemsFoundInDatabaseForFindOneWithVrn();
//
//      // when
//      Optional<CazEntrantPayment> response = vehicleEntrantPaymentRepository
//          .findOnePaidByVrnAndCazEntryDate(UUID.randomUUID(), "VRN", LocalDate.now());
//
//      // then
//      assertThat(response).isEqualTo(Optional.empty());
//    }
    }

    @Nested
    class FindOnePaidByCazEntryDateAndExternalPaymentId {

      @Test
      public void shouldThrowNullPointerExceptionWhenCleanZoneIdIsNull() {
        // given
        UUID cleanZoneId = null;

        // when
        Throwable throwable = catchThrowable(
            () -> cazEntrantPaymentRepository
                .findOnePaidByCazEntryDateAndExternalPaymentId(cleanZoneId, LocalDate.now(),
                    "external_payment_id"));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class)
            .hasMessage("cleanZoneId cannot be null");
      }

      @Test
      public void shouldThrowNullPointerExceptionWhenCazEntryDateIsNull() {
        // given
        LocalDate cazEntryDate = null;

        // when
        Throwable throwable = catchThrowable(
            () -> cazEntrantPaymentRepository
                .findOnePaidByCazEntryDateAndExternalPaymentId(UUID.randomUUID(), cazEntryDate,
                    "external_payment_id"));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class)
            .hasMessage("cazEntryDate cannot be null");
      }

      @Test
      public void shouldThrowNIllegalArgumentExceptionWhenExternalPaymentIsEmpty() {
        // when
        Throwable throwable = catchThrowable(
            () -> cazEntrantPaymentRepository
                .findOnePaidByCazEntryDateAndExternalPaymentId(UUID.randomUUID(), LocalDate.now(),
                    ""));

        // then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("externalPaymentId cannot be empty");
      }
    }
  }
}

//    TODO: Fix with the payment updates CAZ-1716
//    @Test
//    public void shouldThrowNIllegalStateExceptionWhenFoundMoreThanOneVehicleEntrantPayment() {
//      // given
//      mockMultipleItemsFoundInDatabaseForFindOneWithExternalPayment();
//
//      // when
//      Throwable throwable = catchThrowable(
//          () -> vehicleEntrantPaymentRepository
//              .findOnePaidByCazEntryDateAndExternalPaymentId(UUID.randomUUID(), LocalDate.now(),
//                  "test"));
//
//      // then
//      assertThat(throwable).isInstanceOf(NotUniqueVehicleEntrantPaymentFoundException.class)
//          .hasMessage("Not able to find unique VehicleEntrantPayment");
//    }

//    @Test
//    public void shouldReturnOptionalEmptyWhenNoVehicleEntrantPaymentFound() {
//      // given
//      mockNoItemsFoundInDatabaseForFindOneWithExternalPayment();
//
//      // when
//      Optional<CazEntrantPayment> response = vehicleEntrantPaymentRepository
//          .findOnePaidByCazEntryDateAndExternalPaymentId(UUID.randomUUID(), LocalDate.now(),
//              "test");
//
//      // then
//      assertThat(response).isEqualTo(Optional.empty());
//    }
//  }

//  private void mockMultipleItemsFoundInDatabaseForFindOneWithExternalPayment() {
//    List<CazEntrantPayment> vehicleEntrantPayments = CazEntrantPayments.forRandomDays();
//
//    when(jdbcTemplate.query(
//        eq(VehicleEntrantPaymentRepository.SELECT_BY_EXTERNAL_PAYMENT_VRN_AND_STATUS_SQL),
//        any(PreparedStatementSetter.class),
//        any(VehicleEntrantPaymentRowMapper.class)
//    )).thenReturn(vehicleEntrantPayments);
//  }

//  private void mockMultipleItemsFoundInDatabaseForFindOneWithVrn() {
//    List<CazEntrantPayment> vehicleEntrantPayments = CazEntrantPayments.forRandomDays();
//
//    when(jdbcTemplate.query(
//        eq(CazEntrantPaymentRepository.SELECT_BY_VRN_CAZ_ENTRY_DATE_AND_STATUS_SQL),
//        any(PreparedStatementSetter.class),
//        any(CazEntrantPaymentRepository.class)
//    )).thenReturn(vehicleEntrantPayments);
//  }

//  private void mockNoItemsFoundInDatabaseForFindOneWithExternalPayment() {
//    when(jdbcTemplate.query(
//        eq(VehicleEntrantPaymentRepository.SELECT_BY_EXTERNAL_PAYMENT_VRN_AND_STATUS_SQL),
//        any(PreparedStatementSetter.class),
//        any(VehicleEntrantPaymentRowMapper.class)
//    )).thenReturn(Collections.emptyList());
//  }

//  private void mockNoItemsFoundInDatabaseForFindOneWithVrn() {
//    when(jdbcTemplate.query(
//        eq(CazEntrantPaymentRepository.SELECT_BY_VRN_CAZ_ENTRY_DATE_AND_STATUS_SQL),
//        any(PreparedStatementSetter.class),
//        any(CazEntrantPaymentRowMapper.class)
//    )).thenReturn(Collections.emptyList());
//  }
//}
