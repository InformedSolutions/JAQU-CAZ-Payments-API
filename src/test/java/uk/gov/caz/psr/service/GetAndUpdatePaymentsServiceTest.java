package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.gov.caz.psr.domain.authentication.CredentialRetrievalManager;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.dto.external.PaymentState;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.service.VehicleEntrantPaymentsService;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.GetPaymentResultConverter;
import uk.gov.caz.psr.util.TestObjectFactory;
import uk.gov.caz.psr.util.TestObjectFactory.ExternalPaymentDetailsFactory;

@ExtendWith(MockitoExtension.class)
class GetAndUpdatePaymentsServiceTest {

  @Mock
  private PaymentRepository internalPaymentsRepository;

  @Mock
  private ExternalPaymentsRepository externalPaymentsRepository;

  @Mock
  private PaymentStatusUpdater paymentStatusUpdater;

  @Mock
  private CredentialRetrievalManager credentialRetrievalManager;

  @Mock
  private VehicleEntrantPaymentsService vehicleEntrantPaymentsService;

  @Mock
  private GetPaymentResultConverter getPaymentResultConverter;

  @InjectMocks
  private GetAndUpdatePaymentsService getAndUpdatePaymentsService;

  private final UUID cazIdentifier = UUID.fromString("ab3e9f4b-4076-4154-b6dd-97c5d4800b47");

  @Test
  public void shouldThrowNullPointerExceptionWhenPassedNullValue() {
    // given
    UUID id = null;

    // when
    Throwable throwable =
        catchThrowable(() -> getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(id));

    assertThat(throwable).isInstanceOf(NullPointerException.class).hasMessage("ID cannot be null");
  }

  @Test
  public void shouldThrowIllegalStateExceptionWhenPaymentIsNotFoundExternally() {
    // given
    UUID paymentId = UUID.fromString("c56fcd5f-fde6-4d7d-aa3f-8ff192a6244f");
    String externalId = "ext-id";
    Payment payment = mockInternalPaymentWith(paymentId, externalId);
    mockApiKey(payment);
    mockPaymentAbsenceInExternalService(payment);

    // when
    Throwable throwable = catchThrowable(
        () -> getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(paymentId));

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("External payment not found with id");
  }

  @Test
  public void shouldReturnEmptyOptionalIfPaymentIsNotFoundInDatabase() {
    // given
    UUID paymentId = UUID.fromString("a80d2cc8-f97a-11e9-9272-1b75c20437eb");
    given(internalPaymentsRepository.findById(paymentId)).willReturn(Optional.empty());

    // when
    Optional<Payment> result =
        getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(paymentId);

    // then
    assertThat(result).isEmpty();
    verify(externalPaymentsRepository, never()).findById(any());
    verify(paymentStatusUpdater, never()).updateWithExternalPaymentDetails(any(), any());
  }

  @Test
  public void shouldNotUpdatePaymentStatusIfExternalPaymentIdIsNull() {
    // given
    UUID paymentId = UUID.fromString("c56fcd5f-fde6-4d7d-aa3f-8ff192a6244f");
    String externalId = null;
    mockInternalPaymentWith(paymentId, externalId);

    // when
    Optional<Payment> result =
        getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(paymentId);

    // then
    assertThat(result).isEmpty();
    verify(externalPaymentsRepository, never()).findById(any());
    verify(paymentStatusUpdater, never()).updateWithExternalPaymentDetails(any(), any());
  }

  @Test
  public void shouldNotUpdatePaymentStatusIfExternalPaymentStatusNotChanged() {
    UUID paymentId = UUID.fromString("c56fcd5f-fde6-4d7d-aa3f-8ff192a6244f");
    String externalId = "ext-id";
    Payment payment = mockInternalPaymentWith(paymentId, externalId);
    mockApiKey(payment);
    mockSameExternalStatusFor(payment);
    mockGetPaymentResultConverter(payment.getExternalPaymentStatus());

    // when
    Optional<Payment> result =
        getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(paymentId);

    // then
    assertThat(result).contains(payment);
    verify(paymentStatusUpdater, never()).updateWithExternalPaymentDetails(any(), any());
  }

  @Test
  public void shouldUpdatePaymentStatusIfExternalPaymentStatusChanged() {
    // given
    ExternalPaymentDetails initExternalPaymentDetails =
        ExternalPaymentDetailsFactory.anyWithStatus(ExternalPaymentStatus.CREATED);
    ExternalPaymentDetails newExternalPaymentDetails =
        ExternalPaymentDetailsFactory.anyWithStatus(ExternalPaymentStatus.SUCCESS);

    String email = "example@email.com";
    UUID paymentId = UUID.fromString("c56fcd5f-fde6-4d7d-aa3f-8ff192a6244f");
    Payment payment = mockInternalPaymentWith(paymentId, "ext-id",
        initExternalPaymentDetails.getExternalPaymentStatus());
    mockApiKey(payment);
    Payment paymentWithEmail = payment.toBuilder().emailAddress(email).build();
    mockSuccessStatusFor(payment, email);
    mockStatusUpdaterWithSuccess(paymentWithEmail, newExternalPaymentDetails);
    mockGetPaymentResultConverter(ExternalPaymentStatus.SUCCESS);

    // when
    Optional<Payment> result =
        getAndUpdatePaymentsService.getExternalPaymentAndUpdateStatus(paymentId);

    // then
    assertThat(result).contains(paymentWithEmail);
    verify(paymentStatusUpdater).updateWithExternalPaymentDetails(eq(paymentWithEmail),
        eq(newExternalPaymentDetails));
  }

  private void mockStatusUpdaterWithSuccess(Payment payment,
      ExternalPaymentDetails externalPaymentDetails) {
    given(paymentStatusUpdater.updateWithExternalPaymentDetails(payment, externalPaymentDetails))
        .willAnswer(answer -> answer.getArgument(0));
  }

  private Payment mockInternalPaymentWith(UUID paymentId, String externalId,
      ExternalPaymentStatus newStatus) {
    Payment payment = TestObjectFactory.Payments.forRandomDaysWithId(paymentId, externalId, null)
        .toBuilder().externalPaymentStatus(newStatus).build();
    mockInternalPaymentInDatabase(paymentId, payment);
    return payment;
  }

  private void mockInternalPaymentInDatabase(UUID paymentId, Payment payment) {
    given(internalPaymentsRepository.findById(paymentId)).willReturn(Optional.of(payment));
  }

  private void mockGetPaymentResultConverter(ExternalPaymentStatus externalPaymentStatus) {
    given(getPaymentResultConverter.toExternalPaymentDetails(any()))
        .willReturn(ExternalPaymentDetails.builder().email("example@email.com")
            .externalPaymentStatus(externalPaymentStatus).build());
  }

  private void mockSuccessStatusFor(Payment payment, String email) {
    given(externalPaymentsRepository.findById(payment.getExternalId()))
        .willReturn(Optional.of(GetPaymentResult.builder().email(email)
            .state(PaymentState.builder().status(ExternalPaymentStatus.SUCCESS.name()).build())
            .build()));
  }

  private void mockExternalStatusFor(Payment payment, ExternalPaymentStatus status) {
    given(externalPaymentsRepository.findById(payment.getExternalId()))
        .willReturn(Optional.of(GetPaymentResult.builder()
            .state(PaymentState.builder().status(status.name()).build()).build()));
  }

  private void mockSameExternalStatusFor(Payment payment) {
    mockExternalStatusFor(payment, payment.getExternalPaymentStatus());
  }

  private Payment mockInternalPaymentWith(UUID paymentId, String externalId) {
    Payment payment =
        TestObjectFactory.Payments.forRandomDaysWithId(paymentId, externalId, cazIdentifier);
    mockInternalPaymentInDatabase(paymentId, payment);
    return payment;
  }

  private void mockPaymentAbsenceInExternalService(Payment payment) {
    given(externalPaymentsRepository.findById(payment.getExternalId()))
        .willReturn(Optional.empty());
  }

  private void mockApiKey(Payment payment) {
    given(vehicleEntrantPaymentsService.findCazId(payment.getVehicleEntrantPayments()))
        .willReturn(Optional.of(cazIdentifier));
    given(credentialRetrievalManager.getApiKey(cazIdentifier))
        .willReturn(Optional.of("testApiKey"));
  }
}
