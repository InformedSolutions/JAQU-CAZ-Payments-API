package uk.gov.caz.psr.service;

import com.google.common.collect.Iterables;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.repository.PaymentStatusRepository;

/**
 * Class responsible to call internal repository for payment info.
 */
@Service
@AllArgsConstructor
public class ChargeSettlementService {

  private final PaymentStatusRepository paymentStatusRepository;

  /**
   * Method that call internal repository and detects if the payment was done against the CAZ entry
   * represented by the passed attributes.
   *
   * @param cazId          provided clean air zone ID
   * @param vrn            of the car that needs payment info
   * @param dateOfCazEntry for which payment info is searched
   * @return {@link PaymentStatus}
   * @throws IllegalStateException when single day was paid twice
   */
  public PaymentStatus findChargeSettlement(UUID cazId, String vrn,
      LocalDate dateOfCazEntry) {

    Collection<PaymentStatus> paymentStatuses = paymentStatusRepository
        .findByCazIdAndVrnAndEntryDate(cazId, vrn, dateOfCazEntry);
    Collection<PaymentStatus> paidPaymentStatuses = getPaidPaymentStatuses(paymentStatuses);

    return getPaymentStatusToReturn(paymentStatuses, paidPaymentStatuses);
  }

  /**
   * Method receives a collection of PaymentStatuses and returns only those with status PAID.
   *
   * @param paymentStatuses all PaymentStatuses to filter
   * @return collection of {@link PaymentStatus} with PAID statuses
   */
  private Collection<PaymentStatus> getPaidPaymentStatuses(
      Collection<PaymentStatus> paymentStatuses) {

    return paymentStatuses
        .stream()
        .filter(p -> p.getStatus() == InternalPaymentStatus.PAID)
        .collect(Collectors.toList());
  }

  /**
   * Method decides which PaymentStatus object should be returned.
   *
   * @param allPaymentStatuses  all PaymentStatuses returned by repository
   * @param paidPaymentStatuses filtered PaymentStatuses with PAID status
   * @return {@link PaymentStatus}
   */
  private PaymentStatus getPaymentStatusToReturn(Collection<PaymentStatus> allPaymentStatuses,
      Collection<PaymentStatus> paidPaymentStatuses) {
    if (paidPaymentStatuses.size() > 1) {
      throw new IllegalStateException("More than one paid VehicleEntrantPayment found");
    }

    if (paidPaymentStatuses.size() == 1) {
      return paidPaymentStatuses.iterator().next();
    }

    return Iterables.getFirst(allPaymentStatuses, PaymentStatus.getEmptyPaymentStatusResponse());
  }
}
