package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
<<<<<<< HEAD
=======
import static org.mockito.Mockito.when;

>>>>>>> 0b3427216898cdca1b2a03074aed4b81f521254a
import java.time.LocalDate;
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
import org.springframework.web.client.RestClientException;
import uk.gov.caz.psr.domain.authentication.CredentialRetrievalManager;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
public class InitiatePaymentServiceTest {

  @Mock
  private ExternalPaymentsRepository externalPaymentsRepository;

  @Mock
  private PaymentRepository internalPaymentsRepository;

  @Mock
<<<<<<< HEAD
  private CredentialRetrievalManager credentialRetrievalManager;
=======
  private VehicleEntrantPaymentChargeCalculator chargeCalculator;
>>>>>>> 0b3427216898cdca1b2a03074aed4b81f521254a

  @InjectMocks
  private InitiatePaymentService initiatePaymentService;

  @Test
  public void shouldReturnPaymentWithExternalId() {
    // given
<<<<<<< HEAD
    String correlationId = "CORRELATION_ID";
    String cleanAirZoneName = "test";
    InitiatePaymentRequest request = createRequest(cleanAirZoneName);
    Payment paymentWithoutInternalId =
        createPaymentWithoutId(request, correlationId);
    Payment paymentWithInternalId =
        mockPaymentWithoutExternalDetails(paymentWithoutInternalId);
    Payment paymentWithExternalId =
        mockSuccessPaymentCreation(paymentWithInternalId, request);
    mockApiKeyRetrieval(cleanAirZoneName);

    // when
    Payment result =
        initiatePaymentService.createPayment(request, correlationId);

    // then
    assertThat(result).isEqualTo(paymentWithExternalId);
    verify(internalPaymentsRepository).insert(paymentWithoutInternalId);
    verify(externalPaymentsRepository).create(paymentWithInternalId,
        request.getReturnUrl());
=======
    InitiatePaymentRequest request = createRequest();
    Payment paymentWithoutInternalId = createPaymentWithoutId(request);
    Payment paymentWithInternalId = mockPaymentWithoutExternalDetails(paymentWithoutInternalId);
    Payment paymentWithExternalId = mockSuccessPaymentCreation(paymentWithInternalId, request);
    mockChargeCalculator(request);

    // when
    Payment result = initiatePaymentService.createPayment(request);

    // then
    assertThat(result).isEqualTo(paymentWithExternalId);
    verify(internalPaymentsRepository).insertWithExternalStatus(paymentWithoutInternalId);
    verify(externalPaymentsRepository).create(paymentWithInternalId, request.getReturnUrl());
>>>>>>> 0b3427216898cdca1b2a03074aed4b81f521254a
    verify(internalPaymentsRepository).update(paymentWithExternalId);
  }

  @Test
  public void shouldNotUpdatePaymentWhenExternalPaymentCreationFailed() {
    // given
<<<<<<< HEAD
    String correlationId = "CORRELATION_ID";
    String cleanAirZoneName = "test";
    InitiatePaymentRequest request = createRequest(cleanAirZoneName);
    Payment paymentWithoutInternalId =
        createPaymentWithoutId(request, correlationId);
    Payment paymentWithInternalId =
        mockPaymentWithoutExternalDetails(paymentWithoutInternalId);
    mockFailedPaymentCreation(paymentWithInternalId, request);
    mockApiKeyRetrieval(cleanAirZoneName);
=======
    InitiatePaymentRequest request = createRequest();
    Payment paymentWithoutInternalId = createPaymentWithoutId(request);
    Payment paymentWithInternalId = mockPaymentWithoutExternalDetails(paymentWithoutInternalId);
    mockFailedPaymentCreation(paymentWithInternalId, request);
    mockChargeCalculator(request);
>>>>>>> 0b3427216898cdca1b2a03074aed4b81f521254a

    // when
    Throwable throwable = catchThrowable(() -> initiatePaymentService.createPayment(request));

    // then
    assertThat(throwable).isInstanceOf(RestClientException.class);
<<<<<<< HEAD
    verify(internalPaymentsRepository).insert(paymentWithoutInternalId);
    verify(externalPaymentsRepository).create(paymentWithInternalId,
        request.getReturnUrl());
    verify(internalPaymentsRepository, never()).update(any());
  }

  private InitiatePaymentRequest createRequest(String cleanAirZoneName) {
    List<LocalDate> days =
        Arrays.asList(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3));

    return InitiatePaymentRequest.builder().cleanAirZoneName(cleanAirZoneName)
        .days(days).vrn("VRN123").amount(1234)
        .returnUrl("https://example.return.url").build();
  }

  private String mockApiKeyRetrieval(String cleanAirZoneName) {
    given(credentialRetrievalManager.getApiKey(cleanAirZoneName))
        .willReturn(Optional.of("test-api-key"));
    return "test-api-key";
  }

  private Payment createPaymentWithoutId(InitiatePaymentRequest request,
      String correlationId) {
    return Payment.builder().status(PaymentStatus.INITIATED)
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .cleanAirZoneId(request.getCleanAirZoneId())
        .chargePaid(request.getAmount()).correlationId(correlationId).build();
=======
    verify(internalPaymentsRepository).insertWithExternalStatus(paymentWithoutInternalId);
    verify(externalPaymentsRepository).create(paymentWithInternalId, request.getReturnUrl());
    verify(internalPaymentsRepository, never()).update(any());
  }

  private InitiatePaymentRequest createRequest() {
    List<LocalDate> days = Arrays
        .asList(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3));

    return InitiatePaymentRequest.builder()
        .cleanAirZoneId(UUID.randomUUID())
        .days(days)
        .vrn("VRN123")
        .amount(700)
        .returnUrl("https://example.return.url")
        .build();
  }

  private Payment createPaymentWithoutId(InitiatePaymentRequest request) {
    return Payments.forRequest(request);
>>>>>>> 0b3427216898cdca1b2a03074aed4b81f521254a
  }

  private Payment mockPaymentWithoutExternalDetails(Payment paymentWithoutId) {
    Payment paymentWithId = toPaymentWithId(paymentWithoutId);

<<<<<<< HEAD
    given(internalPaymentsRepository.insert(paymentWithoutId))
        .willReturn(paymentWithId);
=======
    given(internalPaymentsRepository.insertWithExternalStatus(paymentWithoutId)).willReturn(paymentWithId);
>>>>>>> 0b3427216898cdca1b2a03074aed4b81f521254a

    return paymentWithId;
  }

  private Payment mockSuccessPaymentCreation(Payment payment,
      InitiatePaymentRequest request) {
    Payment externalPayment = toPaymentWithExternalPaymentDetails(payment);
    given(externalPaymentsRepository.create(payment, request.getReturnUrl()))
        .willReturn(externalPayment);
    return externalPayment;
  }

<<<<<<< HEAD
  private void mockFailedPaymentCreation(Payment payment,
      InitiatePaymentRequest request) {
    Payment externalPayment = toPaymentWithExternalPaymentDetails(payment);
    given(externalPaymentsRepository.create(payment, request.getReturnUrl()))
        .willThrow(new RestClientException(any()));
  }

  private Payment toPaymentWithId(Payment payment) {
    return payment.toBuilder().id(UUID.randomUUID()).build();
  }

  private Payment toPaymentWithExternalPaymentDetails(Payment payment) {
    return payment.toBuilder().externalPaymentId("ANY_EXTERNAL_ID")
        .status(PaymentStatus.CREATED).build();
=======
  private void mockFailedPaymentCreation(Payment payment, InitiatePaymentRequest request) {
    given(externalPaymentsRepository.create(payment, request.getReturnUrl())).willThrow(
        new RestClientException(""));
  }

  private Payment toPaymentWithId(Payment payment) {
    UUID paymentID = UUID.randomUUID();

    return payment.toBuilder()
        .id(paymentID)
        .vehicleEntrantPayments(payment.getVehicleEntrantPayments()
            .stream()
            .map(vehicleEntrantPayment -> vehicleEntrantPayment.toBuilder()
                .paymentId(paymentID)
                .build())
            .collect(Collectors.toList()))
        .build();
  }

  private Payment toPaymentWithExternalPaymentDetails(Payment payment) {
    return payment.toBuilder()
        .externalId("ANY_EXTERNAL_ID")
        .externalPaymentStatus(ExternalPaymentStatus.CREATED)
        .build();
>>>>>>> 0b3427216898cdca1b2a03074aed4b81f521254a
  }

  private void mockChargeCalculator(InitiatePaymentRequest request) {
    when(chargeCalculator.calculateCharge(anyInt(), anyInt())).thenReturn(
        request.getAmount() / request.getDays().size()
    );
  }
}
