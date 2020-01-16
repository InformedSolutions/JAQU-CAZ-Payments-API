package uk.gov.caz.psr.service;

import com.google.common.collect.Iterables;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
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

    return Iterables.getFirst(paymentStatuses, PaymentStatus.getEmptyPaymentStatusResponse());
  }
}
