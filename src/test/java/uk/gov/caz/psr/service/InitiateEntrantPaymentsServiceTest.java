package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
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
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentMatch;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.Payment.PaymentBuilder;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.SingleEntrantPayment;
import uk.gov.caz.psr.repository.EntrantPaymentMatchRepository;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class InitiateEntrantPaymentsServiceTest {

  private static final int ANY_CHARGE = 10;

  @Mock
  private EntrantPaymentRepository entrantPaymentRepository;
  @Mock
  private EntrantPaymentMatchRepository entrantPaymentMatchRepository;
  @Mock
  private PaymentRepository paymentRepository;
  @Mock
  private CleanupDanglingPaymentService cleanupDanglingPaymentService;

  @InjectMocks
  private InitiateEntrantPaymentsService service;
  public static final String ANY_VRN = "ABCDEF";
  public static final UUID ANY_CLEAN_AIR_ZONE_ID = UUID
      .fromString("cf54fd70-3902-11ea-a0e6-fbf575fb00ee");
  public static final String ANY_TARIFF_CODE = "my-tariff";
  public static final LocalDate ANY_TRAVEL_DATE = LocalDate.now();
  public static final UUID ANY_PAYMENT_ID = UUID.fromString("b71b72a5-902f-4a16-a91d-1a4463b801db");

  @Nested
  class WhenThereAreNoEntrantPaymentsForTravelDays {

    @Test
    public void shouldInsertEntrantPaymentsAndEntrantPaymentMatches() {
      // given
      List<LocalDate> travelDates = Collections.singletonList(ANY_TRAVEL_DATE);
      List<SingleEntrantPayment> transactions = Collections.singletonList(SingleEntrantPayment.builder()
              .charge(ANY_CHARGE)
              .travelDate(ANY_TRAVEL_DATE)
              .vrn(ANY_VRN)
              .tariffCode(ANY_TARIFF_CODE)
              .build());

      mockNoCurrentEntrantPaymentsInDatabase(travelDates, ANY_VRN, ANY_CLEAN_AIR_ZONE_ID);
      UUID entrantPaymentId = mockEntrantPaymentRepository();

      // when
      service.processEntrantPaymentsForPayment(ANY_PAYMENT_ID, ANY_CLEAN_AIR_ZONE_ID, transactions);

      // then
      verify(entrantPaymentRepository).insert(argThat((EntrantPayment entrantPayment) ->
          entrantPayment.getUpdateActor() == EntrantPaymentUpdateActor.USER
              && entrantPayment.getInternalPaymentStatus() == InternalPaymentStatus.NOT_PAID
              && ANY_TARIFF_CODE.equals(entrantPayment.getTariffCode())
              && ANY_CHARGE == entrantPayment.getCharge()
              && ANY_TRAVEL_DATE.equals(entrantPayment.getTravelDate())
              && ANY_CLEAN_AIR_ZONE_ID.equals(entrantPayment.getCleanAirZoneId())
              && ANY_VRN.equals(entrantPayment.getVrn())
      ));
      verify(entrantPaymentMatchRepository)
          .insert(argThat((EntrantPaymentMatch entrantPaymentMatch) ->
              entrantPaymentMatch.getPaymentId().equals(ANY_PAYMENT_ID)
                  && entrantPaymentMatch.getVehicleEntrantPaymentId().equals(entrantPaymentId)
                  && entrantPaymentMatch.isLatest()
          ));
    }
  }

  @Nested
  class WhenThereAreExistingEntrantPaymentsForTravelDates {

    @Nested
    class WhenRelatedPaymentsAreFinished {

      @Test
      public void shouldInsertNotMatchingAndUpdateMatchingOnes() {
        // given
        LocalDate notMatchingDate = LocalDate.now();
        LocalDate matchingDate = LocalDate.now().minusDays(1);
        List<SingleEntrantPayment> transactions = Arrays.asList(SingleEntrantPayment.builder()
                .charge(ANY_CHARGE)
                .travelDate(matchingDate)
                .vrn(ANY_VRN)
                .tariffCode(ANY_TARIFF_CODE)
                .build(),
            SingleEntrantPayment.builder()
                .charge(ANY_CHARGE)
                .travelDate(notMatchingDate)
                .vrn(ANY_VRN)
                .tariffCode(ANY_TARIFF_CODE)
                .build()
        );
        List<LocalDate> travelDates = Arrays.asList(matchingDate, notMatchingDate);
        UUID existingEntrantPaymentId = UUID.fromString("d34f9d3a-54c7-40e3-9611-60caa783a8b7");
        EntrantPayment existingEntrantPayment = mockExistingEntrantPayments(matchingDate,
            travelDates, existingEntrantPaymentId);
        mockEntrantPaymentRepository();
        mockPaymentRepositoryWithFinishedPayment(existingEntrantPaymentId);

        // when
        service.processEntrantPaymentsForPayment(ANY_PAYMENT_ID, ANY_CLEAN_AIR_ZONE_ID, transactions);

        // then
        verify(entrantPaymentRepository).update(argThat((EntrantPayment entrantPayment) ->
            entrantPayment.getCharge() == ANY_CHARGE
                && ANY_TARIFF_CODE.equals(entrantPayment.getTariffCode())
                && EntrantPaymentUpdateActor.USER == entrantPayment.getUpdateActor()
                && entrantPayment.getCleanAirZoneEntrantPaymentId()
                .equals(existingEntrantPayment.getCleanAirZoneEntrantPaymentId())
        ));
        verify(entrantPaymentMatchRepository).updateLatestToFalseFor(existingEntrantPaymentId);
        verify(entrantPaymentRepository).insert(argThat((EntrantPayment entrantPayment) ->
            entrantPayment.getUpdateActor() == EntrantPaymentUpdateActor.USER
                && entrantPayment.getInternalPaymentStatus() == InternalPaymentStatus.NOT_PAID
                && ANY_TARIFF_CODE.equals(entrantPayment.getTariffCode())
                && ANY_CHARGE == entrantPayment.getCharge()
                && notMatchingDate.equals(entrantPayment.getTravelDate())
                && ANY_CLEAN_AIR_ZONE_ID.equals(entrantPayment.getCleanAirZoneId())
                && ANY_VRN.equals(entrantPayment.getVrn())
        ));
        verify(entrantPaymentMatchRepository, times(travelDates.size()))
            .insert(argThat((EntrantPaymentMatch entrantPaymentMatch) ->
                entrantPaymentMatch.getPaymentId().equals(ANY_PAYMENT_ID)
                    && entrantPaymentMatch.isLatest()
            ));
      }
    }

    @Nested
    class WhenRelatedPaymentHasAlreadyBeenPaid {

      @Test
      public void shouldThrowIllegalStateException() {
        // given
        LocalDate notMatchingDate = LocalDate.now();
        LocalDate matchingDate = LocalDate.now().minusDays(1);
        List<SingleEntrantPayment> transactions = Arrays.asList(SingleEntrantPayment.builder()
                .charge(ANY_CHARGE)
                .travelDate(matchingDate)
                .vrn(ANY_VRN)
                .tariffCode(ANY_TARIFF_CODE)
                .build(),
            SingleEntrantPayment.builder()
                .charge(ANY_CHARGE)
                .travelDate(notMatchingDate)
                .vrn(ANY_VRN)
                .tariffCode(ANY_TARIFF_CODE)
                .build()
        );
        List<LocalDate> travelDates = Arrays.asList(matchingDate, notMatchingDate);
        UUID existingEntrantPaymentId = UUID.fromString("d34f9d3a-54c7-40e3-9611-60caa783a8b7");
        mockExistingEntrantPaymentsWithPaidStatus(matchingDate, travelDates,
            existingEntrantPaymentId);

        // when
        Throwable throwable = catchThrowable(() -> service.processEntrantPaymentsForPayment(
            ANY_PAYMENT_ID, ANY_CLEAN_AIR_ZONE_ID, transactions));

        // then
        assertThat(throwable).isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("Cannot process the payment as the entrant on");
        verify(entrantPaymentRepository, never()).insert(any(EntrantPayment.class));
        verify(entrantPaymentMatchRepository, never())
            .updateLatestToFalseFor(existingEntrantPaymentId);
        verify(entrantPaymentRepository, never()).update(any(EntrantPayment.class));
        verify(entrantPaymentMatchRepository, never()).insert(any());
      }
    }

    @Nested
    class WhenRelatedPaymentIsNotFinished {

      @Nested
      class WhenRelatedPaymentHasFailedExternally {

        @Test
        public void shouldCompleteWithoutThrowingAnyException() {
          // given
          LocalDate matchingDate = LocalDate.now().minusDays(1);
          List<LocalDate> travelDates = Arrays.asList(matchingDate);
          List<SingleEntrantPayment> transactions = Arrays.asList(SingleEntrantPayment.builder()
                  .charge(ANY_CHARGE)
                  .travelDate(matchingDate)
                  .vrn(ANY_VRN)
                  .tariffCode(ANY_TARIFF_CODE)
                  .build()
          );
          UUID existingEntrantPaymentId = UUID.fromString("d34f9d3a-54c7-40e3-9611-60caa783a8b7");
          mockExistingEntrantPayments(matchingDate, travelDates, existingEntrantPaymentId);
          mockPaymentRepositoryWithNotFinishedPayment(existingEntrantPaymentId);
          mockExternalPaymentWithFailureStatus();

          // when && then
          assertThatCode(() -> service.processEntrantPaymentsForPayment(
              ANY_PAYMENT_ID, ANY_CLEAN_AIR_ZONE_ID, transactions)
          ).doesNotThrowAnyException();
        }
      }

      @Nested
      class WhenRelatedPaymentHasSucceededExternally {

        @Test
        public void shouldThrowIllegalStateException() {
          // given
          LocalDate notMatchingDate = LocalDate.now();
          LocalDate matchingDate = LocalDate.now().minusDays(1);
          List<SingleEntrantPayment> transactions = Arrays.asList(SingleEntrantPayment.builder()
                  .charge(ANY_CHARGE)
                  .travelDate(matchingDate)
                  .vrn(ANY_VRN)
                  .tariffCode(ANY_TARIFF_CODE)
                  .build(),
              SingleEntrantPayment.builder()
                  .charge(ANY_CHARGE)
                  .travelDate(notMatchingDate)
                  .vrn(ANY_VRN)
                  .tariffCode(ANY_TARIFF_CODE)
                  .build()
          );
          List<LocalDate> travelDates = Arrays.asList(matchingDate, notMatchingDate);
          UUID existingEntrantPaymentId = UUID.fromString("d34f9d3a-54c7-40e3-9611-60caa783a8b7");
          mockExistingEntrantPayments(matchingDate, travelDates, existingEntrantPaymentId);
          mockPaymentRepositoryWithNotFinishedPayment(existingEntrantPaymentId);
          mockExternalPaymentWithSuccessStatus();

          // when
          Throwable throwable = catchThrowable(() -> service.processEntrantPaymentsForPayment(
              ANY_PAYMENT_ID, ANY_CLEAN_AIR_ZONE_ID, transactions));

          // then
          assertThat(throwable).isInstanceOf(IllegalStateException.class)
              .hasMessageStartingWith("The corresponding payment has been completed or not finished");
          verify(entrantPaymentRepository, never()).insert(any(EntrantPayment.class));
          verify(entrantPaymentMatchRepository, never())
              .updateLatestToFalseFor(existingEntrantPaymentId);
          verify(entrantPaymentRepository, never()).update(any(EntrantPayment.class));
          verify(entrantPaymentMatchRepository, never()).insert(any());
        }
      }

      private void mockExternalPaymentWithFailureStatus() {
        when(cleanupDanglingPaymentService.processDanglingPayment(any())).thenAnswer(answer -> {
          Payment payment = answer.getArgument(0);
          return payment.toBuilder()
              .externalPaymentStatus(ExternalPaymentStatus.FAILED)
              .build();
        });
      }

      private void mockExternalPaymentWithSuccessStatus() {
        when(cleanupDanglingPaymentService.processDanglingPayment(any())).thenAnswer(answer -> {
          Payment payment = answer.getArgument(0);
          return payment.toBuilder()
              .externalPaymentStatus(ExternalPaymentStatus.SUCCESS)
              .authorisedTimestamp(LocalDateTime.now())
              .build();
        });
      }

      @Nested
      class WhenRelatedPaymentHasNotCompletedExternally {

        @Test
        public void shouldThrowIllegalStateException() {
          // given
          LocalDate notMatchingDate = LocalDate.now();
          LocalDate matchingDate = LocalDate.now().minusDays(1);
          List<SingleEntrantPayment> transactions = Arrays.asList(SingleEntrantPayment.builder()
                  .charge(ANY_CHARGE)
                  .travelDate(matchingDate)
                  .vrn(ANY_VRN)
                  .tariffCode(ANY_TARIFF_CODE)
                  .build(),
              SingleEntrantPayment.builder()
                  .charge(ANY_CHARGE)
                  .travelDate(notMatchingDate)
                  .vrn(ANY_VRN)
                  .tariffCode(ANY_TARIFF_CODE)
                  .build()
          );
          List<LocalDate> travelDates = Arrays.asList(matchingDate, notMatchingDate);
          UUID existingEntrantPaymentId = UUID.fromString("d34f9d3a-54c7-40e3-9611-60caa783a8b7");
          mockExistingEntrantPayments(matchingDate, travelDates, existingEntrantPaymentId);
          mockPaymentRepositoriesWithNotFinishedPayment(existingEntrantPaymentId);

        // when
        Throwable throwable = catchThrowable(() -> service.processEntrantPaymentsForPayment(
            ANY_PAYMENT_ID, ANY_CLEAN_AIR_ZONE_ID, transactions));

          // then
          assertThat(throwable).isInstanceOf(IllegalStateException.class)
              .hasMessageStartingWith("The corresponding payment has been completed or not finished");
          verify(entrantPaymentRepository, never()).insert(any(EntrantPayment.class));
          verify(entrantPaymentMatchRepository, never())
              .updateLatestToFalseFor(existingEntrantPaymentId);
          verify(entrantPaymentRepository, never()).update(any(EntrantPayment.class));
          verify(entrantPaymentMatchRepository, never()).insert(any());
        }
      }
    }
  }

  private void mockPaymentRepositoryWithNotFinishedPayment(UUID existingEntrantPaymentId) {
    Payment existingPayment = createPayment(ExternalPaymentStatus.INITIATED).build();
    when(paymentRepository.findByEntrantPayment(existingEntrantPaymentId))
        .thenReturn(Optional.of(existingPayment));
  }

  private void mockPaymentRepositoriesWithNotFinishedPayment(UUID existingEntrantPaymentId) {
    Payment existingPayment = createPayment(ExternalPaymentStatus.INITIATED).build();
    mockPaymentRepositoryWithNotFinishedPayment(existingEntrantPaymentId);
    when(cleanupDanglingPaymentService.processDanglingPayment(any())).thenReturn(existingPayment);
  }

  private void mockPaymentRepositoryWithFinishedPayment(UUID existingEntrantPaymentId) {
    Payment existingPayment = createPayment(ExternalPaymentStatus.SUCCESS)
        .authorisedTimestamp(LocalDateTime.now())
        .build();
    when(paymentRepository.findByEntrantPayment(existingEntrantPaymentId))
        .thenReturn(Optional.of(existingPayment));
  }

  private PaymentBuilder createPayment(ExternalPaymentStatus status) {
    return Payment.builder()
        .externalId("ext-id")
        .entrantPayments(Collections.emptyList())
        .totalPaid(ANY_CHARGE)
        .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD)
        .externalPaymentStatus(status);
  }

  private void mockExistingEntrantPaymentsWithPaidStatus(LocalDate matchingDate,
      List<LocalDate> travelDates, UUID existingEntrantPaymentId) {
    EntrantPayment existingEntrantPayment = existingEntrantPaymentWithPaidStatus(matchingDate,
        existingEntrantPaymentId);
    when(entrantPaymentRepository.findByVrnAndCazEntryDates(ANY_CLEAN_AIR_ZONE_ID, ANY_VRN,
        travelDates)).thenReturn(Collections.singletonList(existingEntrantPayment));
  }

  private EntrantPayment mockExistingEntrantPayments(LocalDate matchingDate,
      List<LocalDate> travelDates,
      UUID existingEntrantPaymentId) {
    EntrantPayment existingEntrantPayment = existingEntrantPayment(matchingDate,
        existingEntrantPaymentId);
    when(entrantPaymentRepository
        .findByVrnAndCazEntryDates(ANY_CLEAN_AIR_ZONE_ID, ANY_VRN, travelDates))
        .thenReturn(Collections.singletonList(existingEntrantPayment));
    return existingEntrantPayment;
  }

  private EntrantPayment existingEntrantPaymentWithPaidStatus(LocalDate matchingDate,
      UUID existingEntrantPaymentId) {
    return existingEntrantPayment(matchingDate, existingEntrantPaymentId,
        InternalPaymentStatus.PAID);
  }

  private EntrantPayment existingEntrantPayment(LocalDate matchingDate,
      UUID existingEntrantPaymentId,
      InternalPaymentStatus paymentStatus) {
    return EntrantPayment.builder()
        .vrn(ANY_VRN)
        .charge(ANY_CHARGE)
        .updateActor(EntrantPaymentUpdateActor.USER)
        .cleanAirZoneId(ANY_CLEAN_AIR_ZONE_ID)
        .cleanAirZoneEntrantPaymentId(existingEntrantPaymentId)
        .internalPaymentStatus(paymentStatus)
        .travelDate(matchingDate)
        .build();
  }

  private EntrantPayment existingEntrantPayment(LocalDate matchingDate,
      UUID existingEntrantPaymentId) {
    return existingEntrantPayment(matchingDate, existingEntrantPaymentId,
        InternalPaymentStatus.NOT_PAID);
  }

  private UUID mockEntrantPaymentRepository() {
    UUID cleanAirZoneEntrantPaymentId = UUID.fromString("491f15db-4247-4a25-9eaa-a8842b46d733");
    when(entrantPaymentRepository.insert(any(EntrantPayment.class))).thenAnswer(invocation -> {
      EntrantPayment argument = invocation.getArgument(0);
      return argument.toBuilder()
          .cleanAirZoneEntrantPaymentId(cleanAirZoneEntrantPaymentId)
          .build();
    });
    return cleanAirZoneEntrantPaymentId;
  }

  private void mockNoCurrentEntrantPaymentsInDatabase(List<LocalDate> travelDates, String vrn,
      UUID cleanAirZoneId) {
    when(entrantPaymentRepository.findByVrnAndCazEntryDates(cleanAirZoneId, vrn, travelDates))
        .thenReturn(Collections.emptyList());
  }
}