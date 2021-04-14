package uk.gov.caz.psr.repository.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;

@ExtendWith(MockitoExtension.class)
class PaymentDetailRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private PaymentDetailRepository paymentDetailRepository;

  @Nested
  class GetPaymentStatusesForPaymentIds {

    @Nested
    class WhenPaymentIdsAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        Set<UUID> paymentIds = null;
        EntrantPaymentUpdateActor updateActor = EntrantPaymentUpdateActor.LA;
        List<InternalPaymentStatus> paymentStatuses = Arrays
            .asList(InternalPaymentStatus.REFUNDED, InternalPaymentStatus.CHARGEBACK);

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .getPaymentStatusesForPaymentIds(paymentIds, updateActor, paymentStatuses));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("paymentIds cannot be null");
      }
    }

    @Nested
    class WhenUpdateActorAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        Set<UUID> paymentIds = Collections.emptySet();
        EntrantPaymentUpdateActor updateActor = null;
        List<InternalPaymentStatus> paymentStatuses = Arrays
            .asList(InternalPaymentStatus.REFUNDED, InternalPaymentStatus.CHARGEBACK);

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .getPaymentStatusesForPaymentIds(paymentIds, updateActor, paymentStatuses));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("updateActor cannot be null");
      }
    }

    @Nested
    class WhenPaymentStatusesAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        Set<UUID> paymentIds = Collections.emptySet();
        EntrantPaymentUpdateActor updateActor = EntrantPaymentUpdateActor.LA;
        List<InternalPaymentStatus> paymentStatuses = null;

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .getPaymentStatusesForPaymentIds(paymentIds, updateActor, paymentStatuses));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("paymentStatuses cannot be null");
      }
    }
  }

  @Nested
  class FindAllForPaymentHistory {

    @Nested
    class WhenPaymentIdAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        UUID paymentId = null;
        EntrantPaymentUpdateActor updateActor = EntrantPaymentUpdateActor.LA;
        List<InternalPaymentStatus> paymentStatuses = Arrays
            .asList(InternalPaymentStatus.REFUNDED, InternalPaymentStatus.CHARGEBACK);

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .findAllForPaymentHistory(paymentId, updateActor, paymentStatuses));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("paymentId cannot be null");
      }
    }

    @Nested
    class WhenUpdateActorAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        UUID paymentId = UUID.randomUUID();
        EntrantPaymentUpdateActor updateActor = null;
        List<InternalPaymentStatus> paymentStatuses = Arrays
            .asList(InternalPaymentStatus.REFUNDED, InternalPaymentStatus.CHARGEBACK);

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .findAllForPaymentHistory(paymentId, updateActor, paymentStatuses));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("updateActor cannot be null");
      }
    }

    @Nested
    class WhenPaymentStatusesAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        UUID paymentId = UUID.randomUUID();
        EntrantPaymentUpdateActor updateActor = EntrantPaymentUpdateActor.LA;
        List<InternalPaymentStatus> paymentStatuses = null;

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .findAllForPaymentHistory(paymentId, updateActor, paymentStatuses));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("paymentStatuses cannot be null");
      }
    }
  }

  @Nested
  class FindAllForPaymentsHistory {

    @Nested
    class WhenPaymentIdsAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        List<UUID> paymentIds = null;
        EntrantPaymentUpdateActor updateActor = EntrantPaymentUpdateActor.LA;
        List<InternalPaymentStatus> paymentStatuses = Arrays
            .asList(InternalPaymentStatus.REFUNDED, InternalPaymentStatus.CHARGEBACK);

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .findAllForPaymentsHistory(paymentIds, updateActor, paymentStatuses));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("paymentIds cannot be null");
      }
    }

    @Nested
    class WhenUpdateActorAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        List<UUID> paymentIds = Arrays.asList(UUID.randomUUID());
        EntrantPaymentUpdateActor updateActor = null;
        List<InternalPaymentStatus> paymentStatuses = Arrays
            .asList(InternalPaymentStatus.REFUNDED, InternalPaymentStatus.CHARGEBACK);

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .findAllForPaymentsHistory(paymentIds, updateActor, paymentStatuses));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("updateActor cannot be null");
      }
    }

    @Nested
    class WhenPaymentStatusesAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        List<UUID> paymentIds = Arrays.asList(UUID.randomUUID());
        EntrantPaymentUpdateActor updateActor = EntrantPaymentUpdateActor.LA;
        List<InternalPaymentStatus> paymentStatuses = null;

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .findAllForPaymentsHistory(paymentIds, updateActor, paymentStatuses));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("paymentStatuses cannot be null");
      }
    }
  }

  @Nested
  class GetPaymentStatusesForVrn {

    @Nested
    class WhenVrnAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        String vrn = null;
        Set<UUID> cazIds = Collections.emptySet();
        Set<LocalDate> travelDates = Collections.emptySet();
        Set<UUID> paymentIds = Collections.emptySet();

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .getPaymentStatusesForVrn(vrn, cazIds, travelDates, paymentIds));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("vrn cannot be null");
      }
    }

    @Nested
    class WhenCazIdsAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        String vrn = "VRN123";
        Set<UUID> cazIds = null;
        Set<LocalDate> travelDates = Collections.emptySet();
        Set<UUID> paymentIds = Collections.emptySet();

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .getPaymentStatusesForVrn(vrn, cazIds, travelDates, paymentIds));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("cazIds cannot be null");
      }
    }

    @Nested
    class WhenTravelDatesAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        String vrn = "VRN123";
        Set<UUID> cazIds = Collections.emptySet();
        Set<LocalDate> travelDates = null;
        Set<UUID> paymentIds = Collections.emptySet();

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .getPaymentStatusesForVrn(vrn, cazIds, travelDates, paymentIds));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("travelDates cannot be null");
      }
    }

    @Nested
    class WhenPaymentIdsAreNotProvided {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        String vrn = "VRN123";
        Set<UUID> cazIds = Collections.emptySet();
        Set<LocalDate> travelDates = Collections.emptySet();
        Set<UUID> paymentIds = null;

        // when
        Throwable throwable = catchThrowable(
            () -> paymentDetailRepository
                .getPaymentStatusesForVrn(vrn, cazIds, travelDates, paymentIds));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class);
        assertThat(throwable).hasMessage("paymentIds cannot be null");
      }
    }
  }
}