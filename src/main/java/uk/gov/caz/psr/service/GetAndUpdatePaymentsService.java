package uk.gov.caz.psr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
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
 * Class that handles getting external payments and updating its status in the
 * database.
 */
@Service
@Slf4j
public class GetAndUpdatePaymentsService {

  private final ExternalPaymentsRepository externalPaymentsRepository;
  private final FinalizePaymentService finalizePaymentService;
  private final PaymentRepository internalPaymentsRepository;
  private final MessagingClient messagingClient;
  private final UpdatePaymentWithExternalDataService updatePaymentWithExternalDataService;

  @Value("${services.sqs.template-id}")
  String templateId;

  /**
   * Constructor.
   */
  public GetAndUpdatePaymentsService(
      ExternalPaymentsRepository externalPaymentsRepository,
      FinalizePaymentService finalizePaymentService,
      PaymentRepository internalPaymentsRepository,
      MessagingClient messagingClient,
      UpdatePaymentWithExternalDataService updatePaymentWithExternalDataService) {
    this.externalPaymentsRepository = externalPaymentsRepository;
    this.finalizePaymentService = finalizePaymentService;
    this.internalPaymentsRepository = internalPaymentsRepository;
    this.messagingClient = messagingClient;
    this.updatePaymentWithExternalDataService =
        updatePaymentWithExternalDataService;
  }

  /**
   * Retrieves the payment by its internal identifier and, provided it exists
   * and has an external id, gets its external status and updates it in the
   * database. Then the updated value is returned to the caller.
   * {@link Optional#empty()} is returned if the record is not found in the
   * database or it has {@code null} external identifier.
   *
   * @throws NullPointerException if {@code id} is null
   */
  public Optional<Payment> getExternalPaymentAndUpdateStatus(UUID id) {
    Preconditions.checkNotNull(id, "ID cannot be null");

    Payment internalPayment =
        internalPaymentsRepository.findById(id).orElse(null);

    if (internalPayment == null) {
      log.info("Payment '{}' is absent in the database", id);
      return Optional.empty();
    }

    String externalPaymentId = internalPayment.getExternalId();
    if (externalPaymentId == null) {
      log.info(
          "Payment '{} does not have an external id and its status will not be updated",
          id);
      return Optional.empty();
    }

    return Optional.of(updatePaymentWithExternalDataService
        .updatePaymentWithExternalData(internalPayment));
  }
}
