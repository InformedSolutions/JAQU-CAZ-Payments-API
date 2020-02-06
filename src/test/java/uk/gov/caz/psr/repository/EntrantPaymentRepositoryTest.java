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
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.util.TestObjectFactory.EntrantPayments;

@ExtendWith(MockitoExtension.class)
class EntrantPaymentRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private EntrantPaymentRepository entrantPaymentRepository;

  @Nested
  class InsertList {

    @Test
    public void shouldThrowNullPointerExceptionWhenInsertListIsNull() {
      // given
      List<EntrantPayment> input = null;

      // when
      Throwable throwable = catchThrowable(() -> entrantPaymentRepository.insert(input));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Entrant payments cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenInsertListIsEmpty() {
      // given
      List<EntrantPayment> input = Collections.emptyList();

      // when
      Throwable throwable = catchThrowable(() -> entrantPaymentRepository.insert(input));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Entrant payments cannot be empty");
    }
  }

  @Nested
  class Insert {

    @Test
    public void shouldThrowNullPointerExceptionWhenObjectIsNull() {
      // given
      EntrantPayment cazEntrantPayment = null;

      // when
      Throwable throwable = catchThrowable(
          () -> entrantPaymentRepository.insert(cazEntrantPayment));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Entrant payment cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenObjectHasIdAssigned() {
      // given
      UUID id = UUID.fromString("b37d37e0-ff12-420c-bc75-4dfe8080ac45");
      EntrantPayment cazEntrantPayment = EntrantPayments.anyNotPaid().toBuilder()
          .cleanAirZoneEntrantPaymentId(id).build();

      // when
      Throwable throwable = catchThrowable(
          () -> entrantPaymentRepository.insert(cazEntrantPayment));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Entrant payment cannot have non-null ID");
    }
  }

  @Nested
  class UpdateList {

    @Test
    public void shouldThrowNullPointerExceptionWhenListIsNull() {
      // given
      List<EntrantPayment> input = null;

      // when
      Throwable throwable = catchThrowable(() -> entrantPaymentRepository.update(input));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Entrant payments cannot be null");
    }
  }

  @Nested
  class Update {

    @Test
    public void shouldThrowNullPointerExceptionWhenCazEntrantPaymentIsNull() {
      // given
      EntrantPayment entrantPayment = null;

      // when
      Throwable throwable = catchThrowable(
          () -> entrantPaymentRepository.update(entrantPayment));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Entrant payments cannot be null");
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
          () -> entrantPaymentRepository.findSuccessfullyPaid(vehicleEntrant));

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
          () -> entrantPaymentRepository
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
          () -> entrantPaymentRepository
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
          () -> entrantPaymentRepository
              .findOneByVrnAndCazEntryDate(UUID.randomUUID(), "", LocalDate.now()));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("VRN cannot be empty");
    }

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
  class FindOneByVrnAndCaz {

    @Test
    public void shouldThrowNullPointerExceptionWhenCleanZoneIdIsNull() {
      // given
      UUID cleanZoneId = null;

      // when
      Throwable throwable = catchThrowable(
          () -> entrantPaymentRepository
              .countByVrnAndCaz(cleanZoneId, "VRN123"));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("cleanZoneId cannot be null");
    }
    
    @Test
    public void shouldThrowNIllegalArgumentExceptionWhenVrnIsEmpty() {
      // given
      String vrn = null;

      // when
      Throwable throwable = catchThrowable(
          () -> entrantPaymentRepository
              .countByVrnAndCaz(UUID.randomUUID(), vrn));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("VRN cannot be empty");
    }
  }
  @Nested
  class FindOnePaidByCazEntryDateAndExternalPaymentId {

    @Test
    public void shouldThrowNullPointerExceptionWhenCleanZoneIdIsNull() {
      // given
      UUID cleanZoneId = null;

      // when
      Throwable throwable = catchThrowable(
          () -> entrantPaymentRepository
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
          () -> entrantPaymentRepository
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
          () -> entrantPaymentRepository
              .findOnePaidByCazEntryDateAndExternalPaymentId(UUID.randomUUID(), LocalDate.now(),
                  ""));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("externalPaymentId cannot be empty");
    }
  }

  @Nested
  class FindByPaymentId {

    @Test
    public void shouldThrowNullPointerExceptionWhenPaymentIdIsNull() {
      // given
      UUID paymentId = null;

      // when
      Throwable throwable = catchThrowable(
          () -> entrantPaymentRepository.findByPaymentId(paymentId));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("'paymentId' cannot be null");
    }
  }

  @Nested
  class FindAllPaidByVrnAndDateRangeAndCazId {

    @Test
    public void shouldThrowNullPointerExceptionWhenVrnIsNull() {
      // given
      String vrn = null;
      LocalDate startDate = LocalDate.now();
      LocalDate endDate = LocalDate.now();
      UUID cleanAirZoneId = UUID.randomUUID();

      // when
      Throwable throwable = catchThrowable(
          () -> entrantPaymentRepository.findAllPaidByVrnAndDateRangeAndCazId(
              vrn, startDate, endDate, cleanAirZoneId));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("VRN cannot be empty");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenDateIsNull() {
      // given
      String vrn = "CAS123";
      LocalDate startDate = null;
      LocalDate endDate = LocalDate.now();
      UUID cleanAirZoneId = UUID.randomUUID();

      // when
      Throwable throwable = catchThrowable(
          () -> entrantPaymentRepository.findAllPaidByVrnAndDateRangeAndCazId(
              vrn, startDate, endDate, cleanAirZoneId));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("startDate cannot be null");

    }

    @Test
    public void shouldThrowNullPointerExceptionWhenCazIdIsNull() {
      // given
      String vrn = "CAS123";
      LocalDate startDate = LocalDate.now();
      LocalDate endDate = LocalDate.now();
      UUID cleanAirZoneId = null;

      // when
      Throwable throwable = catchThrowable(
          () -> entrantPaymentRepository.findAllPaidByVrnAndDateRangeAndCazId(
              vrn, startDate, endDate, cleanAirZoneId));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("cleanAirZoneId cannot be null");
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
