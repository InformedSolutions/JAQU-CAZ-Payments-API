package uk.gov.caz.psr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.messaging.MessagingClient;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * A class which is responsible for getting an external status of a given
 * payment, updating it in the database and, if applicable, connecting it to an
 * existing vehicle entrant entity.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class UpdatePaymentWithExternalDataService {

  private final ExternalPaymentsRepository externalPaymentsRepository;
  private final FinalizePaymentService finalizePaymentService;
  private final PaymentRepository internalPaymentsRepository;
  private final MessagingClient messagingClient;

  @Value("${services.sqs.template-id}")
  String templateId;

  /**
   * Gets an external status of {@code payment}, updates it in the database and,
   * if applicable, associates it with an existing vehicle entrant entity.
   *
   * @throws NullPointerException if {@code payment} is null
   * @throws NullPointerException if {@link Payment#getExternalId()} is null
   */
  public Payment updatePaymentWithExternalData(Payment payment) {
    Preconditions.checkNotNull(payment, "Payment cannot be null");
    Preconditions.checkNotNull(payment.getExternalId(),
        "External id cannot be null");

    String externalPaymentId = payment.getExternalId();
    Payment externalPayment = externalPaymentsRepository
        .findById(externalPaymentId)
        .map(getPaymentResult -> handleExternalResponse(payment,
            getPaymentResult))
        .orElseThrow(() -> new IllegalStateException(
            "External payment not found whereas the " + "internal one with id '"
                + payment.getId() + "' and external id " + "'"
                + externalPaymentId + "' exists"));

    return finalizePaymentProcessing(payment, externalPayment);
  }

  /**
   * Handles the information received from the response of the external payment
   * provider.
   */
  private Payment handleExternalResponse(Payment internalPayment,
      GetPaymentResult getPaymentResult) {
    if (getPaymentResult.getPaymentStatus()
        .equals(ExternalPaymentStatus.SUCCESS)) {
      try {
        sendPaymentReceipt(getPaymentResult.getEmail(),
            getPaymentResult.getAmount());
      } catch (JsonProcessingException e) {
        // will only occur if amount is invalid
        log.error(e.getMessage());
        log.warn("Email receipt not sent for payment: {}",
            internalPayment.getId());
      }
    }
    return updateInternalPaymentWith(internalPayment, getPaymentResult);
  }

  /**
   * Creates a SendEmailRequest object and submits it to the messaging client.
   * 
   * @param  email                   the recipient of the email
   * @param  amount                  the total cost of their CAZ charge
   * @throws JsonProcessingException if the amount cannot be serialized into a
   *                                   json string
   */
  private void sendPaymentReceipt(String email, int amount)
      throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, String> personalisationMap = new HashMap<String, String>();
    personalisationMap.put("amount", Integer.toString(amount));

    String personalisation =
        objectMapper.writeValueAsString(personalisationMap);
    SendEmailRequest sendEmailRequest = new SendEmailRequest(this.templateId,
        email, personalisation, UUID.randomUUID().toString());

    messagingClient.publishMessage(sendEmailRequest);
  }

  /**
   * Creates a lambda expression that updates the passed instance of
   * {@link Payment} with the {@code
   * id}.
   */
  private Payment updateInternalPaymentWith(Payment payment,
      GetPaymentResult paymentInfo) {
    ExternalPaymentStatus newStatus = paymentInfo.getPaymentStatus();
    return payment.toBuilder().externalPaymentStatus(newStatus)
        .authorisedTimestamp(getAuthorisedTimestamp(payment, newStatus))
        .vehicleEntrantPayments(
            payment.getVehicleEntrantPayments().stream()
                .map(vehicleEntrantPayment -> vehicleEntrantPayment.toBuilder()
                    .internalPaymentStatus(
                        InternalPaymentStatus.from(newStatus))
                    .build())
                .collect(Collectors.toList()))
        .build();
  }

  /**
   * Updates payment's status in the database if changed and connects to an
   * existing vehicle entrant record if payment has been successfully processed.
   *
   * @param  internalPayment An instance of {@link Payment} with its internal
   *                           identifier set.
   * @param  externalPayment An instance of {@link Payment} with its internal
   *                           and external identifiers set.
   * @return                 An instance of {@link Payment} with vehicle
   *                         entrants' ids set (if applicable).
   */
  private Payment finalizePaymentProcessing(Payment internalPayment,
      Payment externalPayment) {
    if (internalPayment.getExternalPaymentStatus() == externalPayment
        .getExternalPaymentStatus()) {
      log.info(
          "External payment status is the same as the one from database and equal to '{}', "
              + "skipping updating the database",
          internalPayment.getExternalPaymentStatus());
      return externalPayment;
    }
    log.info(
        "Found the external payment, updating its status to '{}' in the database, "
            + "current status: '{}'",
        externalPayment.getExternalPaymentStatus(),
        internalPayment.getExternalPaymentStatus());
    Payment paymentWithVehicleEntrantsIds =
        finalizePaymentService.connectExistingVehicleEntrants(externalPayment);
    internalPaymentsRepository.update(paymentWithVehicleEntrantsIds);
    return paymentWithVehicleEntrantsIds;
  }

  /**
   * Returns {@link LocalDateTime#now()} as the authorised payment timestamp
   * (date and time when the payment has been successfully processed by GOV UK
   * Pay) provided {@code newExternalStatus} is equal to
   * {@link ExternalPaymentStatus#SUCCESS}. Otherwise, the current value is
   * obtained from {@code payment}.
   */
  private LocalDateTime getAuthorisedTimestamp(Payment payment,
      ExternalPaymentStatus newExternalStatus) {
    if (newExternalStatus == ExternalPaymentStatus.SUCCESS) {
      return LocalDateTime.now();
    }
    return payment.getAuthorisedTimestamp();
  }
}
