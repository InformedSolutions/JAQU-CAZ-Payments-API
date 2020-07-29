package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.EntrantPaymentWithLatestPaymentDetailsDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.TestObjectFactory;

@ExtendWith(MockitoExtension.class)
class EntrantPaymentServiceTest {

  private List<VehicleEntrantDto> cazEntrantPaymentDtos;
  private List<EntrantPaymentWithLatestPaymentDetailsDto> response;

  @Mock
  private EntrantPaymentRepository entrantPaymentRepository;

  @Mock
  private PaymentRepository paymentRepository;

  @InjectMocks
  private EntrantPaymentService entrantPaymentService;

  private final static UUID ENTRANT_PAYMENT_ID = UUID.randomUUID();
  private final static String ANY_VRN = "CAS123";
  private final static String ANY_UUID = "6bea485b-7fa6-4b78-9703-b721c98f4b15";
  private final static String ANY_TIMESTAMP = "2020-01-13T12:21:38.234";
  private final static String NOT_PAID_PAYMENT_STATUS = "NOT_PAID";
  private final static String PAID_PAYMENT_STATUS = "PAID";
  private final static String NULL_PAYMENT_METHOD = "null";
  private final static String CARD_PAYMENT_METHOD = "card";
  private final static String DIRECT_DEBIT_PAYMENT_METHOD = "direct_debit";

  @BeforeEach
  public void setup() {
    VehicleEntrantDto dto = buildVehicleEntrantDto();
    cazEntrantPaymentDtos = Arrays.asList(dto);
  }

  @Test
  public void shouldReturnEmptyCollectionWhenProvidedListIsEmpty() {
    // given
    cazEntrantPaymentDtos = new ArrayList<>();

    // when
    callBulkProcess();

    // then
    assertThat(response).isEmpty();
  }

  @Nested
  public class WhenEntrantPayment {

    @Nested
    public class IsNotPresent {

      @Test
      public void thenNewEntrantPaymentShouldBeCreatedAndInsertedIntoDbAndResponseWithOnlyEntrantPaymentDetailsReturned() {
        // given
        mockEmptyCazEntryPaymentRepositoryResponse();

        // when
        callBulkProcess();

        // then
        assertResponseProperties(NOT_PAID_PAYMENT_STATUS, NULL_PAYMENT_METHOD);
        verify(entrantPaymentRepository).insert(buildCazEntrantPaymentToInsert());
        verifyNoInteractions(paymentRepository);
      }
    }

    @Nested
    public class IsPresent {

      @Nested
      public class AndIsPaid {

        @Nested
        public class ButThereIsNoLatestPaymentForThisEntrantPayment {

          @Test
          public void thenResponseWithCorrectEntrantPaymentPropertiesWithNullPaymentMethodShouldBeReturned() {
            // given
            mockNonEmptyPaidCapturedCazEntryPaymentRepositoryResponse();
            mockNoMatchingPayment();

            // when
            callBulkProcess();

            // then
            assertResponseProperties(PAID_PAYMENT_STATUS, NULL_PAYMENT_METHOD);
            verify(paymentRepository).findByEntrantPayment(ENTRANT_PAYMENT_ID);
          }
        }

        @Nested
        public class AndThereIsLatestPaymentForThisEntrantPayment {

          @Nested
          public class PaidByCreditOrDebitCard {

            @Test
            public void thenResponseWithCorrectEntrantPaymentPropertiesWithValidPaymentMethodShouldBeReturned() {
              // given
              mockNonEmptyPaidCapturedCazEntryPaymentRepositoryResponse();
              mockMatchingPaymentWithPaymentMethod(PaymentMethod.CREDIT_DEBIT_CARD);

              // when
              callBulkProcess();

              // then
              assertResponseProperties(PAID_PAYMENT_STATUS, CARD_PAYMENT_METHOD);
              verify(paymentRepository).findByEntrantPayment(ENTRANT_PAYMENT_ID);
            }
          }

          @Nested
          public class PaidByDirectDebit {

            @Test
            public void thenResponseWithCorrectEntrantPaymentPropertiesWithValidPaymentMethodShouldBeReturned() {
              // given
              mockNonEmptyPaidCapturedCazEntryPaymentRepositoryResponse();
              mockMatchingPaymentWithPaymentMethod(PaymentMethod.DIRECT_DEBIT);

              // when
              callBulkProcess();

              // then
              assertResponseProperties(PAID_PAYMENT_STATUS, DIRECT_DEBIT_PAYMENT_METHOD);
              verify(paymentRepository).findByEntrantPayment(ENTRANT_PAYMENT_ID);
            }
          }
        }
      }

      @Nested
      public class ButIsNotPaid {

        @Test
        public void thenResponseWithCorrectEntrantPaymentPropertiesWithNullPaymentMethodShouldBeReturned() {
          // given
          mockNonEmptyCapturedNotPaidCazEntryPaymentRepositoryResponse();

          // when
          callBulkProcess();

          // then
          assertResponseProperties(NOT_PAID_PAYMENT_STATUS, NULL_PAYMENT_METHOD);
          verifyNoInteractions(paymentRepository);
        }
      }

      @Nested
      public class ButVehicleEntrantHasNotBeenCaptured {

        @Test
        public void thenVehicleEntrantCapturedShouldBeUpdated() {
          // given
          mockNonEmptyNotCapturedCazEntryPaymentRepositoryResponse();
          mockNoMatchingPayment();

          // when
          callBulkProcess();

          // then
          assertThat(response).isNotEmpty();
          verify(entrantPaymentRepository).findOneByVrnAndCazEntryDate(
              UUID.fromString(ANY_UUID),
              ANY_VRN,
              LocalDateTime.parse(ANY_TIMESTAMP).toLocalDate()
          );
          verify(entrantPaymentRepository)
              .update(buildCazEntrantPayment(true, InternalPaymentStatus.PAID));
        }
      }

      @Nested
      public class AndVehicleEntrantHasBeenCaptured {

        @Test
        public void thenVehicleEntrantCapturedShouldNotBeUpdated() {
          mockNonEmptyPaidCapturedCazEntryPaymentRepositoryResponse();
          mockNoMatchingPayment();

          // when
          callBulkProcess();

          // then
          assertThat(response).isNotEmpty();
          verify(entrantPaymentRepository).findOneByVrnAndCazEntryDate(
              UUID.fromString(ANY_UUID),
              ANY_VRN,
              LocalDateTime.parse(ANY_TIMESTAMP).toLocalDate()
          );
          verify(paymentRepository).findByEntrantPayment(ENTRANT_PAYMENT_ID);
          verify(entrantPaymentRepository, never())
              .update(buildCazEntrantPayment(true, InternalPaymentStatus.PAID));
        }
      }
    }
  }

  private void mockEmptyCazEntryPaymentRepositoryResponse() {
    when(entrantPaymentRepository.findOneByVrnAndCazEntryDate(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(entrantPaymentRepository.insert(buildCazEntrantPaymentToInsert())).thenReturn(
        buildCazEntrantPaymentToInsert().toBuilder()
            .cleanAirZoneEntrantPaymentId(UUID.fromString(ANY_UUID))
            .build()
    );
  }

  private void mockNonEmptyPaidCapturedCazEntryPaymentRepositoryResponse() {
    when(entrantPaymentRepository.findOneByVrnAndCazEntryDate(any(), any(), any()))
        .thenReturn(Optional.of(buildCazEntrantPayment(true, InternalPaymentStatus.PAID)));
  }

  private void mockNonEmptyCapturedNotPaidCazEntryPaymentRepositoryResponse() {
    when(entrantPaymentRepository.findOneByVrnAndCazEntryDate(any(), any(), any()))
        .thenReturn(Optional.of(buildCazEntrantPayment(true, InternalPaymentStatus.NOT_PAID)));
  }

  private void mockNonEmptyNotCapturedCazEntryPaymentRepositoryResponse() {
    when(entrantPaymentRepository.findOneByVrnAndCazEntryDate(any(), any(), any()))
        .thenReturn(Optional.of(buildCazEntrantPayment(false, InternalPaymentStatus.PAID)));
  }

  private VehicleEntrantDto buildVehicleEntrantDto() {
    return VehicleEntrantDto
        .builder()
        .vrn(ANY_VRN)
        .cazEntryTimestamp(LocalDateTime.parse(ANY_TIMESTAMP))
        .cleanZoneId(UUID.fromString(ANY_UUID))
        .build();
  }

  private EntrantPayment buildCazEntrantPayment(boolean vehicleEntrantCaptured,
      InternalPaymentStatus internalPaymentStatus) {
    return EntrantPayment.builder()
        .cleanAirZoneEntrantPaymentId(ENTRANT_PAYMENT_ID)
        .cleanAirZoneId(UUID.fromString(ANY_UUID))
        .internalPaymentStatus(internalPaymentStatus)
        .vrn(ANY_VRN)
        .vehicleEntrantCaptured(vehicleEntrantCaptured)
        .tariffCode("any-tariff-code")
        .charge(50)
        .travelDate(LocalDateTime.parse(ANY_TIMESTAMP).toLocalDate())
        .cazEntryTimestamp(LocalDateTime.parse(ANY_TIMESTAMP))
        .updateActor(EntrantPaymentUpdateActor.VCCS_API)
        .build();
  }

  private EntrantPayment buildCazEntrantPaymentToInsert() {
    return EntrantPayment.builder()
        .cleanAirZoneId(UUID.fromString(ANY_UUID))
        .internalPaymentStatus(InternalPaymentStatus.NOT_PAID)
        .vrn(ANY_VRN)
        .vehicleEntrantCaptured(true)
        .travelDate(LocalDateTime.parse(ANY_TIMESTAMP).toLocalDate())
        .cazEntryTimestamp(LocalDateTime.parse(ANY_TIMESTAMP))
        .updateActor(EntrantPaymentUpdateActor.VCCS_API)
        .build();
  }

  private void mockNoMatchingPayment() {
    when(paymentRepository.findByEntrantPayment(ENTRANT_PAYMENT_ID)).thenReturn(Optional.empty());
  }

  private void mockMatchingPaymentWithPaymentMethod(PaymentMethod paymentMethod) {
    Payment payment = TestObjectFactory.Payments.existing().toBuilder()
        .paymentMethod(paymentMethod)
        .build();
    when(paymentRepository.findByEntrantPayment(ENTRANT_PAYMENT_ID))
        .thenReturn(Optional.of(payment));
  }

  private void callBulkProcess() {
    response = entrantPaymentService.bulkProcess(cazEntrantPaymentDtos);
  }

  private void assertResponseProperties(String expectedPaymentStatus,
      String expectedPaymentMethod) {
    assertThat(response).isNotEmpty();
    assertThat(response.get(0).getPaymentStatus()).isEqualTo(expectedPaymentStatus);
    assertThat(response.get(0).getPaymentMethod()).isEqualTo(expectedPaymentMethod);
  }
}