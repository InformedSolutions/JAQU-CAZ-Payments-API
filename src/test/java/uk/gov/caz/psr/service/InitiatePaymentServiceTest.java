package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import org.springframework.web.client.RestClientException;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.Transaction;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.ExternalCardPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.InitiatePaymentRequestToModelConverter;
import uk.gov.caz.psr.util.PaymentTransactionsToEntrantsConverter;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
public class InitiatePaymentServiceTest {

  @Mock
  private ExternalCardPaymentsRepository externalCardPaymentsRepository;

  @Mock
  private PaymentRepository internalPaymentsRepository;

  @Mock
  private InitiateEntrantPaymentsService initiateEntrantPaymentsService;

  @InjectMocks
  private InitiatePaymentService initiatePaymentService;

  @Test
  public void shouldReturnPaymentWithExternalId() {
    // given
    InitiatePaymentRequest request = createRequest();
    Payment paymentWithoutInternalId = createPaymentWithoutId(request);
    Payment paymentWithInternalId = mockPaymentWithoutExternalDetails(paymentWithoutInternalId);
    Payment paymentWithExternalId = mockSuccessPaymentCreation(paymentWithInternalId, request);

    // when
    Payment result = initiatePaymentService.createPayment(
        InitiatePaymentRequestToModelConverter.toPayment(request),
        PaymentTransactionsToEntrantsConverter.toSingleEntrantPayments(request.getTransactions()),
        request.getReturnUrl()
    );

    // then
    assertThat(result).isEqualTo(paymentWithExternalId);
    verify(internalPaymentsRepository).insert(paymentWithoutInternalId);
    verify(externalCardPaymentsRepository).create(paymentWithInternalId, request.getReturnUrl());
    verify(internalPaymentsRepository).update(paymentWithExternalId);
    verify(initiateEntrantPaymentsService).processEntrantPaymentsForPayment(
        eq(paymentWithInternalId.getId()), eq(request.getCleanAirZoneId()), anyList()
    );
  }

  @Test
  public void shouldNotUpdatePaymentWhenExternalPaymentCreationFailed() {
    // given
    InitiatePaymentRequest request = createRequest();
    Payment paymentWithoutInternalId = createPaymentWithoutId(request);
    Payment paymentWithInternalId = mockPaymentWithoutExternalDetails(paymentWithoutInternalId);
    mockFailedPaymentCreation(paymentWithInternalId, request);

    // when
    Throwable throwable = catchThrowable(() -> initiatePaymentService.createPayment(
        InitiatePaymentRequestToModelConverter.toPayment(request),
        PaymentTransactionsToEntrantsConverter
            .toSingleEntrantPayments(request.getTransactions()), request.getReturnUrl()
    ));

    // then
    assertThat(throwable).isInstanceOf(RestClientException.class);
    verify(internalPaymentsRepository).insert(paymentWithoutInternalId);
    verify(externalCardPaymentsRepository).create(paymentWithInternalId, request.getReturnUrl());
    verify(internalPaymentsRepository, never()).update(any());
    verify(initiateEntrantPaymentsService, never()).processEntrantPaymentsForPayment(
        eq(paymentWithInternalId.getId()), eq(request.getCleanAirZoneId()), anyList()
    );
  }

  private InitiatePaymentRequest createRequest() {
    List<LocalDate> days = Arrays.asList(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3));

    return InitiatePaymentRequest.builder()
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
        .telephonePayment(Boolean.FALSE)
        .cleanAirZoneId(UUID.randomUUID())
        .returnUrl("https://example.return.url")
        .build();
  }

  private Payment createPaymentWithoutId(InitiatePaymentRequest request) {
    return Payments.forRequest(request);
  }

  private Payment mockPaymentWithoutExternalDetails(Payment paymentWithoutId) {
    Payment paymentWithId = toPaymentWithId(paymentWithoutId);
    given(internalPaymentsRepository.insert(paymentWithoutId))
        .willReturn(paymentWithId);
    return paymentWithId;
  }

  private Payment mockSuccessPaymentCreation(Payment payment, InitiatePaymentRequest request) {
    Payment externalPayment = toPaymentWithExternalPaymentDetails(payment);
    given(externalCardPaymentsRepository.create(payment, request.getReturnUrl()))
        .willReturn(externalPayment);
    return externalPayment;
  }


  private void mockFailedPaymentCreation(Payment payment, InitiatePaymentRequest request) {
    given(externalCardPaymentsRepository.create(payment, request.getReturnUrl()))
        .willThrow(new RestClientException(""));
  }

  private Payment toPaymentWithId(Payment payment) {
    UUID paymentID = UUID.randomUUID();

    return payment.toBuilder()
        .id(paymentID)
        .build();
  }

  private Payment toPaymentWithExternalPaymentDetails(Payment payment) {
    return payment.toBuilder().externalId("ANY_EXTERNAL_ID")
        .externalPaymentStatus(ExternalPaymentStatus.CREATED).build();
  }
}
