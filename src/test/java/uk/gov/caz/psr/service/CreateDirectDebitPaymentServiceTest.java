package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.Transaction;
import uk.gov.caz.psr.dto.directdebit.CreateDirectDebitPaymentRequest;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.ExternalDirectDebitRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.directdebit.DirectDebitPaymentStatusUpdater;
import uk.gov.caz.psr.util.DirectDebitPaymentRequestToModelConverter;
import uk.gov.caz.psr.util.PaymentTransactionsToEntrantsConverter;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class CreateDirectDebitPaymentServiceTest {

  @Mock
  private PaymentRepository paymentsRepository;

  @Mock
  private InitiateEntrantPaymentsService initiateEntrantPaymentsService;

  @Mock
  private ExternalDirectDebitRepository externalDirectDebitRepository;

  @Mock
  private DirectDebitPaymentStatusUpdater directDebitPaymentStatusUpdater;

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
        PaymentTransactionsToEntrantsConverter.toSingleEntrantPayments(request.getTransactions()),
        request.getMandateId()
    ));

    // then
    assertThat(throwable).isInstanceOf(RuntimeException.class)
        .hasMessage("Payment initialization failed");
  }

  private void mockFailedPaymentInDBCreation(Payment payment,
      CreateDirectDebitPaymentRequest request) {
    Payment paymentWithId = payment.toBuilder().id(UUID.randomUUID()).build();
    given(paymentsRepository.insert(payment)).willReturn(paymentWithId);
    doNothing().when(initiateEntrantPaymentsService)
        .processEntrantPaymentsForPayment(paymentWithId.getId(), paymentWithId.getCleanAirZoneId(),
            PaymentTransactionsToEntrantsConverter.toSingleEntrantPayments(request.getTransactions()));
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