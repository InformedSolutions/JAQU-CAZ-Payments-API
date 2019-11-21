package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.repository.VehicleEntrantPaymentRepository.VehicleEntrantPaymentRowMapper;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;
import uk.gov.caz.psr.util.TestObjectFactory.VehicleEntrantPayments;

@ExtendWith(MockitoExtension.class)
class VehicleEntrantPaymentRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private VehicleEntrantPaymentRepository vehicleEntrantPaymentRepository;

  @Nested
  class Insert {

    @Test
    public void shouldThrowNullPointerExceptionWhenInsertListIsNull() {
      // given
      List<VehicleEntrantPayment> input = null;

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntrantPaymentRepository.insert(input));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Vehicle entrant payments cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenInsertListIsEmpty() {
      // given
      List<VehicleEntrantPayment> input = Collections.emptyList();

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntrantPaymentRepository.insert(input));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Vehicle entrant payments cannot be empty");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenInsertListIsContainsNullPaymentId() {
      // given
      List<VehicleEntrantPayment> input = Payments.forRandomDays().getVehicleEntrantPayments();

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntrantPaymentRepository.insert(input));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Each vehicle entrant payment must have 'payment_id' set");
    }
  }

  @Nested
  class UpdateList {

    @Test
    public void shouldThrowNullPointerExceptionWhenListIsNull() {
      // given
      List<VehicleEntrantPayment> input = null;

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntrantPaymentRepository.update(input));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Vehicle entrant payments cannot be null");
    }
  }

  @Nested
  class Update {

    @Test
    public void shouldThrowNullPointerExceptionWhenvVhicleEntrantPaymentIsNull() {
      // given
      VehicleEntrantPayment vehicleEntrantPayment = null;

      // when
      Throwable throwable = catchThrowable(
          () -> vehicleEntrantPaymentRepository.update(vehicleEntrantPayment));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Vehicle entrant payments cannot be null");
    }
  }

  @Nested
  class FindSuccessfullyPaid {

    @Test
    public void shouldThrowNullPointerExceptionWhenVehicleEntrantIsNull() {
      // given
      VehicleEntrant vehicleEntrant = null;

      // when
      Throwable throwable = catchThrowable(
          () -> vehicleEntrantPaymentRepository.findSuccessfullyPaid(vehicleEntrant));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Vehicle Entrant cannot be null");
    }
  }

  @Nested
  class FindOnePaidByVrnAndCazEntryDate {

    @Test
    public void shouldThrowNullPointerExceptionWhenCleanZoneIdIsNull() {
      // given
      UUID cleanZoneId = null;

      // when
      Throwable throwable = catchThrowable(
          () -> vehicleEntrantPaymentRepository
              .findOnePaidByVrnAndCazEntryDate(cleanZoneId, "VRN123", LocalDate.now()));

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
          () -> vehicleEntrantPaymentRepository
              .findOnePaidByVrnAndCazEntryDate(UUID.randomUUID(), "VRN123", cazEntryDate));

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
          () -> vehicleEntrantPaymentRepository
              .findOnePaidByVrnAndCazEntryDate(UUID.randomUUID(), "", LocalDate.now()));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("VRN cannot be empty");
    }

    @Test
    public void shouldThrowNIllegalStateExceptionWhenFoundMoreThanOneVehicleEntrantPayment() {
      // given
      mockMultipleItemsFoundInDatabaseForFindOneWithVrn();

      // when
      Throwable throwable = catchThrowable(
          () -> vehicleEntrantPaymentRepository
              .findOnePaidByVrnAndCazEntryDate(UUID.randomUUID(), "VRN", LocalDate.now()));

      // then
      assertThat(throwable).isInstanceOf(IllegalStateException.class)
          .hasMessage("More than one VehicleEntrantPayment found");
    }

    @Test
    public void shouldReturnOptionalEmptyWhenNoVehicleEntrantPaymentFound() {
      // given
      mockNoItemsFoundInDatabaseForFindOneWithVrn();

      // when
      Optional<VehicleEntrantPayment> response = vehicleEntrantPaymentRepository
          .findOnePaidByVrnAndCazEntryDate(UUID.randomUUID(), "VRN", LocalDate.now());

      // then
      assertThat(response).isEqualTo(Optional.empty());
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
          () -> vehicleEntrantPaymentRepository
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
          () -> vehicleEntrantPaymentRepository
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
          () -> vehicleEntrantPaymentRepository
              .findOnePaidByCazEntryDateAndExternalPaymentId(UUID.randomUUID(), LocalDate.now(),
                  ""));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("externalPaymentId cannot be empty");
    }

    @Test
    public void shouldThrowNIllegalStateExceptionWhenFoundMoreThanOneVehicleEntrantPayment() {
      // given
      mockMultipleItemsFoundInDatabaseForFindOneWithExternalPayment();

      // when
      Throwable throwable = catchThrowable(
          () -> vehicleEntrantPaymentRepository
              .findOnePaidByCazEntryDateAndExternalPaymentId(UUID.randomUUID(), LocalDate.now(),
                  "test"));

      // then
      assertThat(throwable).isInstanceOf(IllegalStateException.class)
          .hasMessage("More than one VehicleEntrantPayment found");
    }

    @Test
    public void shouldReturnOptionalEmptyWhenNoVehicleEntrantPaymentFound() {
      // given
      mockNoItemsFoundInDatabaseForFindOneWithExternalPayment();

      // when
      Optional<VehicleEntrantPayment> response = vehicleEntrantPaymentRepository
          .findOnePaidByCazEntryDateAndExternalPaymentId(UUID.randomUUID(), LocalDate.now(),
              "test");

      // then
      assertThat(response).isEqualTo(Optional.empty());
    }
  }

  private void mockMultipleItemsFoundInDatabaseForFindOneWithExternalPayment() {
    List<VehicleEntrantPayment> vehicleEntrantPayments = VehicleEntrantPayments.forRandomDays();

    when(jdbcTemplate.query(
        eq(VehicleEntrantPaymentRepository.SELECT_BY_EXTERNAL_PAYMENT_VRN_AND_STATUS_SQL),
        any(PreparedStatementSetter.class),
        any(VehicleEntrantPaymentRowMapper.class)
    )).thenReturn(vehicleEntrantPayments);
  }

  private void mockMultipleItemsFoundInDatabaseForFindOneWithVrn() {
    List<VehicleEntrantPayment> vehicleEntrantPayments = VehicleEntrantPayments.forRandomDays();

    when(jdbcTemplate.query(
        eq(VehicleEntrantPaymentRepository.SELECT_BY_VRN_CAZ_ENTRY_DATE_AND_STATUS_SQL),
        any(PreparedStatementSetter.class),
        any(VehicleEntrantPaymentRowMapper.class)
    )).thenReturn(vehicleEntrantPayments);
  }

  private void mockNoItemsFoundInDatabaseForFindOneWithExternalPayment() {
    when(jdbcTemplate.query(
        eq(VehicleEntrantPaymentRepository.SELECT_BY_EXTERNAL_PAYMENT_VRN_AND_STATUS_SQL),
        any(PreparedStatementSetter.class),
        any(VehicleEntrantPaymentRowMapper.class)
    )).thenReturn(Collections.emptyList());
  }

  private void mockNoItemsFoundInDatabaseForFindOneWithVrn() {
    when(jdbcTemplate.query(
        eq(VehicleEntrantPaymentRepository.SELECT_BY_VRN_CAZ_ENTRY_DATE_AND_STATUS_SQL),
        any(PreparedStatementSetter.class),
        any(VehicleEntrantPaymentRowMapper.class)
    )).thenReturn(Collections.emptyList());
  }
}
