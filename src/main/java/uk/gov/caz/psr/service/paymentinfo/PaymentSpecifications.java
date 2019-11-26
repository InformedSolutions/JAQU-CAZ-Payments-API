package uk.gov.caz.psr.service.paymentinfo;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import javax.persistence.criteria.ListJoin;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.VehicleEntrantPayment;

/**
 * dsf sdfdfss dsfsdf.
 */
@AllArgsConstructor
public class PaymentSpecifications {

  public Specification<Payment> specification(PaymentInfoRequest paymentInfoRequest) {
    Queue<Specification<Payment>> predicates = new LinkedList<>();

    if (paymentInfoRequest.getPaymentId() != null) {
      predicates.add(byPaymentId(paymentInfoRequest.getPaymentId()));
    }

    if (paymentInfoRequest.getToDatePaidFor() != null
        && paymentInfoRequest.getFromDatePaidFor() != null) {
      predicates.add(byFromAndToDate(paymentInfoRequest.getFromDatePaidFor(),
          paymentInfoRequest.getToDatePaidFor()));
    } else if (paymentInfoRequest.getToDatePaidFor() != null) {
      predicates.add(byToDate(paymentInfoRequest.getToDatePaidFor()));

    } else if (paymentInfoRequest.getFromDatePaidFor() != null) {
      predicates.add(byFromDate(paymentInfoRequest.getFromDatePaidFor()));
    }

    Iterator<Specification<Payment>> iterator = predicates.iterator();
    Specification<Payment> where = Specification.where(iterator.next());
    while (iterator.hasNext()) {
      where.and(iterator.next());
    }
    return where;
  }

  private Specification<Payment> byFromDate(LocalDate fromDatePaidFor) {
    return (root, query, criteriaBuilder) -> {
      ListJoin<Payment, VehicleEntrantPayment> vehicleEntrantPaymentListJoin = root
          .joinList("vehicleEntrantPayments");
      return criteriaBuilder.and(
          criteriaBuilder.greaterThanOrEqualTo(vehicleEntrantPaymentListJoin.get("travelDate"),
              fromDatePaidFor),
          criteriaBuilder.lessThanOrEqualTo(vehicleEntrantPaymentListJoin.get("travelDate"),
              fromDatePaidFor.plusDays(1)));
    };
  }

  private Specification<Payment> byToDate(LocalDate toDatePaidFor) {
    return (root, query, criteriaBuilder) -> {
      ListJoin<Payment, VehicleEntrantPayment> vehicleEntrantPaymentListJoin = root
          .joinList("vehicleEntrantPayments");
      return criteriaBuilder.and(
          criteriaBuilder.greaterThanOrEqualTo(vehicleEntrantPaymentListJoin.get("travelDate"),
              toDatePaidFor.minusDays(1)),
          criteriaBuilder
              .lessThanOrEqualTo(vehicleEntrantPaymentListJoin.get("travelDate"), toDatePaidFor));
    };
  }

  private Specification<Payment> byFromAndToDate(LocalDate fromDatePaidFor,
      LocalDate toDatePaidFor) {
    return (root, query, criteriaBuilder) -> {
      ListJoin<Payment, VehicleEntrantPayment> vehicleEntrantPaymentListJoin = root
          .joinList("vehicleEntrantPayments");
      return criteriaBuilder.and(
          criteriaBuilder.greaterThanOrEqualTo(vehicleEntrantPaymentListJoin.get("travelDate"),
              fromDatePaidFor),
          criteriaBuilder
              .lessThanOrEqualTo(vehicleEntrantPaymentListJoin.get("travelDate"), toDatePaidFor));
    };
  }


  /**
   * dfsdf.
   *
   * @param externalId dsfgdsf
   * @return sdf
   */
  private static Specification<Payment> byPaymentId(String externalId) {
    return (root, query, criteriaBuilder) -> criteriaBuilder
        .equal(root.get("externalId"), externalId);
  }
}
