package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.Transaction;
import uk.gov.caz.psr.dto.directdebit.CreateDirectDebitPaymentRequest;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.directdebit.DirectDebitPayment;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.directdebit.DirectDebitPaymentFinalizer;
import uk.gov.caz.psr.service.directdebit.DirectDebitPaymentService;
import uk.gov.caz.psr.service.exception.CollectDirectDebitPaymentException;
import uk.gov.caz.psr.util.DirectDebitPaymentRequestToModelConverter;
import uk.gov.caz.psr.util.PaymentTransactionsToEntrantsConverter;
import uk.gov.caz.psr.util.TestObjectFactory.DirectDebitPayments;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class CreateDirectDebitPaymentServiceTest {

  @Mock
  private PaymentRepository paymentsRepository;

  @Mock
  private InitiateEntrantPaymentsService initiateEntrantPaymentsService;

  @Mock
  private DirectDebitPaymentFinalizer directDebitPaymentFinalizer;

  @Mock
  private DirectDebitPaymentService directDebitPaymentService;

  @InjectMocks
  CreateDirectDebitPaymentService createDirectDebitPaymentService;

  @Test
  public void shouldThrowRuntimeExceptionWhenNotAbleToFindCreatedPayment() {
    // given
    CreateDirectDebitPaymentRequest request = createRequest();
    Payment payment = Payments.forDirectDebitRequest(request);
    mockFailedPaymentInDBCreation(payment, request);

    // when
    Throwable throwable = catchThrowable(() -> createDirectDebitPaymentService.createPayment(
        DirectDebitPaymentRequestToModelConverter.toPayment(request),
        PaymentTransactionsToEntrantsConverter.toSingleEntrantPayments(request.getTransactions())
    ));

    // then
    assertThat(throwable).isInstanceOf(RuntimeException.class)
        .hasMessage("Payment initialization failed");
  }

  @Test
  public void shouldThrowExceptionWhenNotAbleToCollectPayment() {
    // given
    CreateDirectDebitPaymentRequest request = createRequest();
    Payment payment = Payments.forDirectDebitRequest(request);
    Payment paymentWithEntrants = mockSuccessPaymentInDBCreation(payment, request);
    mockFailedPaymentCollection(paymentWithEntrants, payment.getCleanAirZoneId());

    // when
    Throwable throwable = catchThrowable(() -> createDirectDebitPaymentService.createPayment(
        DirectDebitPaymentRequestToModelConverter.toPayment(request),
        PaymentTransactionsToEntrantsConverter.toSingleEntrantPayments(request.getTransactions())
    ));

    // then
    assertThat(throwable).isInstanceOf(CollectDirectDebitPaymentException.class);
  }

  @Test
  public void shouldReturnCreatedPayment() {
    // given
    CreateDirectDebitPaymentRequest request = createRequest();
    Payment payment = Payments.forDirectDebitRequest(request);
    Payment paymentWithEntrants = mockSuccessPaymentInDBCreation(payment, request);
    DirectDebitPayment directDebitPayment = mockSuccessPaymentCollection(paymentWithEntrants,
        payment.getCleanAirZoneId());
    Payment finalizedPayment = mockSuccessPaymentFinalization(paymentWithEntrants,
        directDebitPayment, payment.getEmailAddress());
    // when
    Payment result = createDirectDebitPaymentService.createPayment(
        DirectDebitPaymentRequestToModelConverter.toPayment(request),
        PaymentTransactionsToEntrantsConverter.toSingleEntrantPayments(request.getTransactions())
    );

    // then
    assertThat(result).isEqualTo(finalizedPayment);
  }

  private void mockFailedPaymentInDBCreation(Payment payment,
      CreateDirectDebitPaymentRequest request) {
    Payment paymentWithId = payment.toBuilder().id(UUID.randomUUID()).build();
    given(paymentsRepository.insert(payment)).willReturn(paymentWithId);
    doNothing().when(initiateEntrantPaymentsService)
        .processEntrantPaymentsForPayment(paymentWithId.getId(), paymentWithId.getCleanAirZoneId(),
            PaymentTransactionsToEntrantsConverter
                .toSingleEntrantPayments(request.getTransactions()));
  }

  private Payment mockSuccessPaymentInDBCreation(Payment payment,
      CreateDirectDebitPaymentRequest request) {
    Payment paymentWithId = payment.toBuilder().id(UUID.randomUUID()).build();
    Payment paymentWithIdAndEntrants = Payments.existing();
    given(paymentsRepository.insert(payment)).willReturn(paymentWithId);
    doNothing().when(initiateEntrantPaymentsService)
        .processEntrantPaymentsForPayment(paymentWithId.getId(), paymentWithId.getCleanAirZoneId(),
            PaymentTransactionsToEntrantsConverter
                .toSingleEntrantPayments(request.getTransactions()));
    given(paymentsRepository.findById(paymentWithId.getId()))
        .willReturn(Optional.of(paymentWithIdAndEntrants));
    return paymentWithIdAndEntrants;
  }

  private void mockFailedPaymentCollection(Payment payment, UUID cleanAirZoneId) {
    given(directDebitPaymentService
        .collectPayment(payment.getId(), cleanAirZoneId, payment.getTotalPaid(),
            payment.getReferenceNumber(), payment.getPaymentProviderMandateId())).willThrow(
        CollectDirectDebitPaymentException.class);
  }

  private DirectDebitPayment mockSuccessPaymentCollection(Payment payment, UUID cleanAirZoneId) {
    DirectDebitPayment directDebitPayment = DirectDebitPayments.any();
    given(directDebitPaymentService
        .collectPayment(payment.getId(), cleanAirZoneId, payment.getTotalPaid(),
            payment.getReferenceNumber(), payment.getPaymentProviderMandateId()))
        .willReturn(directDebitPayment);
    return directDebitPayment;
  }

  private Payment mockSuccessPaymentFinalization(Payment payment,
      DirectDebitPayment directDebitPayment, String email) {
    Payment finalizedPayment = payment.toBuilder()
        .externalPaymentStatus(ExternalPaymentStatus.SUCCESS)
        .externalId(directDebitPayment.getPaymentId())
        .submittedTimestamp(LocalDateTime.now())
        .authorisedTimestamp(LocalDateTime.now())
        .build();
    given(directDebitPaymentFinalizer
        .finalizeSuccessfulPayment(payment, directDebitPayment.getPaymentId(), email))
        .willReturn(finalizedPayment);
    return finalizedPayment;
  }

  private CreateDirectDebitPaymentRequest createRequest() {
    List<LocalDate> days = Arrays.asList(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3));

    return CreateDirectDebitPaymentRequest.builder()
        .transactions(
            days.stream()
                .map(travelDate -> Transaction.builder()
                    .charge(700)
                    .travelDate(travelDate)
                    .vrn("VRN123")
                    .tariffCode("TARIFF_CODE")
                    .build())
                .collect(Collectors.toList())
        )
        .cleanAirZoneId(UUID.randomUUID())
        .mandateId("exampleMandateId")
        .build();
  }
}