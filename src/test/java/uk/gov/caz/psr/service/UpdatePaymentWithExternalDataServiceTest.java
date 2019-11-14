package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.RestClientException;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.dto.external.PaymentState;
import uk.gov.caz.psr.messaging.MessagingClient;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.TestObjectFactory;

@ExtendWith(MockitoExtension.class)
class UpdatePaymentWithExternalDataServiceTest {

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @Mock
  private ExternalPaymentsRepository externalPaymentsRepository;

  @Mock
  private PaymentRepository internalPaymentsRepository;

  @Mock
  private FinalizePaymentService finalizePaymentService;

  @Mock
  private MessagingClient messagingClient;

  @InjectMocks
  private UpdatePaymentWithExternalDataService service;

  @Test
  public void shouldThrowNullPointerExceptionWhenPaymentIsNull() {
    // given
    Payment payment = null;

    // when
    Throwable throwable = catchThrowable(() -> service.updatePaymentWithExternalData(payment));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Payment cannot be null");
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenExternalPaymentIdIsNull() {
    // given
    Payment payment = createPayment(UUID.randomUUID(), null);

    // when
    Throwable throwable = catchThrowable(() -> service.updatePaymentWithExternalData(payment));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("External id cannot be null");
  }


  @Test
  public void shouldThrowIllegalStateExceptionWhenPaymentWithExtIdExistsButIsNotFoundExternally() {
    // given
    UUID paymentId = UUID.fromString("1ae108cc-fb9d-11e9-8483-67f1dfc0829d");
    String externalId = "external-id-1";
    Payment payment = createPayment(paymentId, externalId);
    mockAbsenceOfExternalPayment(externalId);

    // when
    Throwable throwable = catchThrowable(() -> service.updatePaymentWithExternalData(payment));

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class)
        .hasMessage("External payment not found whereas the internal one with id '%s' "
            + "and external id '%s' exists", paymentId, externalId);
  }

  @Test
  public void shouldUpdatePaymentStatusIfExternalPaymentIsFound() {
    // given
    UUID paymentId = UUID.fromString("744b3067-11aa-463f-b5d7-992196f959c6");
    String externalId = "external-id";
    ExternalPaymentStatus externalStatus = ExternalPaymentStatus.FAILED;
    Payment payment = createPayment(paymentId, externalId);
    mockExternalPaymentWithStatus(externalId, payment, externalStatus);
    mockFinalizePayment();

    // when
    Payment result = service.updatePaymentWithExternalData(payment);

    // then
    Payment internalPaymentWithExternalStatus =
        payment.toBuilder().authorisedTimestamp(null).externalPaymentStatus(externalStatus).build();
    assertThat(result).isEqualTo(internalPaymentWithExternalStatus);
    verify(internalPaymentsRepository).update(internalPaymentWithExternalStatus);
    verify(applicationEventPublisher).publishEvent(any());
  }

  @Test
  public void shouldNotUpdatePaymentIfExternalPaymentIsFoundAndItsStatusMatchesInternalOne() {
    // given
    UUID paymentId = UUID.fromString("744b3067-11aa-463f-b5d7-992196f959c6");
    String externalId = "external-id";
    ExternalPaymentStatus externalStatus = ExternalPaymentStatus.INITIATED;
    Payment payment = createPayment(paymentId, externalId);
    mockExternalPaymentWithStatus(externalId, payment, externalStatus);

    // when
    Payment result = service.updatePaymentWithExternalData(payment);

    // then
    Payment internalPaymentWithExternalStatus =
        payment.toBuilder().externalPaymentStatus(externalStatus).build();
    assertThat(result).isEqualTo(internalPaymentWithExternalStatus);
    verify(internalPaymentsRepository, never()).update(any());
  }

  @Test
  public void shouldSetAuthorisedTimestampForSuccessfulPayment() {
    // given
    UUID paymentId = UUID.fromString("744b3067-11aa-463f-b5d7-992196f959c6");
    String externalId = "external-id";
    Payment payment = createPayment(paymentId, externalId);
    mockSuccessExternalPayment(externalId, payment);
    mockFinalizePayment();

    // when
    Payment result = service.updatePaymentWithExternalData(payment);

    // then
    assertThat(result.getAuthorisedTimestamp()).isNotNull();
    verify(applicationEventPublisher).publishEvent(any());
  }

  @Test
  public void shouldSetInternalStatusInVehicleEntrants() {
    // given
    UUID paymentId = UUID.fromString("744b3067-11aa-463f-b5d7-992196f959c6");
    String externalId = "external-id";
    Payment payment = createPayment(paymentId, externalId);
    mockSuccessExternalPayment(externalId, payment);
    mockFinalizePayment();

    // when
    Payment result = service.updatePaymentWithExternalData(payment);

    // then
    assertThat(result.getVehicleEntrantPayments()).allSatisfy(vehicleEntrantPayment -> {
      InternalPaymentStatus internalStatus = vehicleEntrantPayment.getInternalPaymentStatus();
      assertThat(internalStatus).isEqualTo(InternalPaymentStatus.PAID);
    });
    verify(applicationEventPublisher).publishEvent(any());
  }

  @Test
  public void shouldNotCallInternalRepositoriesWhenGovUkCallFails() {
    // given
    UUID paymentId = UUID.fromString("744b3067-11aa-463f-b5d7-992196f959c6");
    String externalId = "external-id";
    Payment payment = createPayment(paymentId, externalId);
    mockExternalRepoCallFailure();

    // when
    Throwable throwable = catchThrowable(() -> service.updatePaymentWithExternalData(payment));

    // then
    assertThat(throwable).isInstanceOf(RestClientException.class);
    verify(finalizePaymentService, never()).connectExistingVehicleEntrants(any());
    verify(internalPaymentsRepository, never()).update(any());
  }

  private void mockExternalRepoCallFailure() {
    given(externalPaymentsRepository.findById(anyString())).willThrow(new RestClientException(""));
  }

  private void mockFinalizePayment() {
    given(finalizePaymentService.connectExistingVehicleEntrants(any()))
        .willAnswer(i -> i.getArguments()[0]);
  }

  private void mockExternalPaymentWithStatus(String externalId, Payment payment,
      ExternalPaymentStatus status) {
    GetPaymentResult externalPayment = toExternalPaymentWithStatus(payment, status);
    mockExternalPaymentResult(externalId, externalPayment);
  }

  private void mockSuccessExternalPayment(String externalId, Payment payment) {
    GetPaymentResult externalPayment =
        toExternalPaymentBuilderWithStatus(payment, ExternalPaymentStatus.SUCCESS).build();
    mockExternalPaymentResult(externalId, externalPayment);
  }

  private void mockExternalPaymentResult(String externalId, GetPaymentResult externalPayment) {
    given(externalPaymentsRepository.findById(externalId)).willReturn(Optional.of(externalPayment));
  }

  private GetPaymentResult toExternalPaymentWithStatus(Payment payment,
      ExternalPaymentStatus status) {
    return toExternalPaymentBuilderWithStatus(payment, status).build();
  }

  private GetPaymentResult.GetPaymentResultBuilder toExternalPaymentBuilderWithStatus(
      Payment payment, ExternalPaymentStatus status) {
    return GetPaymentResult.builder().paymentId(payment.getExternalId())
        .state(PaymentState.builder().status(status.name().toLowerCase()).build());
  }

  private void mockAbsenceOfExternalPayment(String externalId) {
    given(externalPaymentsRepository.findById(externalId)).willReturn(Optional.empty());
  }

  private Payment createPayment(UUID paymentId, String externalId) {
    return TestObjectFactory.Payments.forRandomDaysWithId(paymentId, externalId);
  }
}
