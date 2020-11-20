package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.messaging.MessagingClient;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.exception.PaymentNotFoundException;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptServiceTest {

  private static final Long ANY_PAYMENT_REFERENCE = Long.valueOf(2307);
  private static final String ANY_EMAIL = "abcdef@test.com";

  @Mock
  private MessagingClient messagingClient;

  @Mock
  private PaymentReceiptEmailCreator paymentReceiptEmailCreator;

  @Mock
  private PaymentRepository paymentRepository;

  @InjectMocks
  private PaymentReceiptService paymentReceiptService;

  @Test
  public void shouldThrowExceptionWhenPaymentDoesNotExist() {
    // given
    when(paymentRepository.findByReferenceNumber(any())).thenReturn(Optional.empty());

    // when
    Throwable throwable = catchThrowable(
        () -> paymentReceiptService.sendReceipt(ANY_PAYMENT_REFERENCE, ANY_EMAIL));

    // then
    assertThat(throwable).isInstanceOf(PaymentNotFoundException.class)
        .hasMessage("Payment with provided reference number does not exist");
  }

  @Test
  public void shouldSendEmailWhenPaymentExists() {
    // given
    when(paymentRepository.findByReferenceNumber(any())).thenReturn(getSamplePayment());

    // when
    paymentReceiptService.sendReceipt(ANY_PAYMENT_REFERENCE, ANY_EMAIL);

    // then
    verify(paymentReceiptEmailCreator).createSendEmailRequest(any());
    verify(messagingClient).publishMessage(any());
  }

  private Optional<Payment> getSamplePayment() {
    Payment payment = Payment.builder()
        .telephonePayment(true)
        .paymentMethod(PaymentMethod.DIRECT_DEBIT)
        .totalPaid(0)
        .entrantPayments(Collections.emptyList())
        .emailAddress(ANY_EMAIL)
        .externalId(RandomStringUtils.randomAlphabetic(3))
        .externalPaymentStatus(ExternalPaymentStatus.INITIATED)
        .referenceNumber(RandomUtils.nextLong())
        .build();

    return Optional.of(payment);
  }
}
