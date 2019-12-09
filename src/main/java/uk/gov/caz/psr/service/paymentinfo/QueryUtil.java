package uk.gov.caz.psr.service.paymentinfo;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.PluralAttribute;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Jpa Specification queries util.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryUtil {

  /**
   * Helper method to create or find existing join, to avoid multiple joins on the same table.
   *
   * @param from {@link Root}
   * @param attribute {@link PluralAttribute}
   * @param <V> parent class of join
   * @param <T> child class of join
   */
  public static <V, T> Join<V, T> getOrCreateJoin(Root<V> from, Attribute attribute) {
    return (Join<V, T>) from.getFetches()
        .stream()
        .filter(fetch -> attribute.getName().equals(fetch.getAttribute().getName()))
        .findFirst()
        .orElseGet(() -> from.fetch(attribute.getName()));
  }
}
