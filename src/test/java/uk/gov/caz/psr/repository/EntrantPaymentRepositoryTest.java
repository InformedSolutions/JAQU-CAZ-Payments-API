package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
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
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.repository.exception.NotUniqueVehicleEntrantPaymentFoundException;
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
  class FindOneByVrnAndCazEntryDate {

    @Test
    public void shouldThrowNotUniqueEntrantPaymentFoundExceptionWhenMoreThanOneRowIsReturned() {
      // given
      mockMoreThanOneRowReturned();

      // when
      Throwable throwable = catchThrowable(() ->
          entrantPaymentRepository.findOneByVrnAndCazEntryDate(UUID.randomUUID(), "VRN123", LocalDate.now()));

      // then
      assertThat(throwable).isInstanceOf(NotUniqueVehicleEntrantPaymentFoundException.class);
    }

    private void mockMoreThanOneRowReturned() {
      when(jdbcTemplate.query(any(String.class), any(PreparedStatementSetter.class), any(RowMapper.class)))
          .thenReturn(Arrays.asList(EntrantPayments.anyPaid(), EntrantPayments.anyPaid()));
    }

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

  @Nested
  class FindByVrnAndCazEntryDates {

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenCazEntryDatesAreNull() {
      // given
      UUID cazId = UUID.randomUUID();
      String vrn = "VRN321";
      List<LocalDate> cazEntryDates = null;

      // when
      Throwable throwable = catchThrowable(() ->
          entrantPaymentRepository.findByVrnAndCazEntryDates(cazId, vrn, cazEntryDates));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("cazEntryDates cannot be null or empty");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenCazEntryDatesAreEmpty() {
      // given
      UUID cazId = UUID.randomUUID();
      String vrn = "VRN321";
      List<LocalDate> cazEntryDates = Collections.emptyList();

      // when
      Throwable throwable = catchThrowable(() ->
          entrantPaymentRepository.findByVrnAndCazEntryDates(cazId, vrn, cazEntryDates));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("cazEntryDates cannot be null or empty");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenVrnIsNull() {
      // given
      UUID cazId = UUID.randomUUID();
      String vrn = null;
      List<LocalDate> cazEntryDates = Collections.singletonList(LocalDate.now());

      // when
      Throwable throwable = catchThrowable(() ->
          entrantPaymentRepository.findByVrnAndCazEntryDates(cazId, vrn, cazEntryDates));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("VRN cannot be empty");
    }
  }
}
