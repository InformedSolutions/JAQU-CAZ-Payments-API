
package uk.gov.caz.psr.model.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.VehicleEntrantPayment;

/**
 * Service class for VehicleEntrantPayment objects.
 * 
 */
@Service
public class VehicleEntrantPaymentsService {

  /**
   * Uses a list of vehicle entrant payments (associated with a payment) to find the Clean Air Zone
   * ID of the payment.
   * 
   * @param vehicleEntrantPayments a list of VehicleEntrantPayment objects
   * @return a unique identifier for a Clean Air Zone
   */
  public UUID findCazId(List<VehicleEntrantPayment> vehicleEntrantPayments) {
    return vehicleEntrantPayments.stream()
        .filter(distinctByKey(VehicleEntrantPayment::getCleanZoneId)).collect(toSingleton())
        .getCleanZoneId();
  }

  private static <T> Collector<T, ?, T> toSingleton() {
    return Collectors.collectingAndThen(Collectors.toList(), list -> {
      if (list.size() != 1) {
        throw new IllegalStateException();
      }
      return list.get(0);
    });
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }
}
