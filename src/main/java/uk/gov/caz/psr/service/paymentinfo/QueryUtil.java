package uk.gov.caz.psr.service.paymentinfo;

import javax.persistence.criteria.CriteriaQuery;
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
    return (Join<V, T>) from.getJoins()
        .stream()
        .filter(fetch -> attribute.getName().equals(fetch.getAttribute().getName()))
        .findFirst()
        .orElseGet(() -> from.join(attribute.getName()));
  }

  /**
   * Helper method to create or find existing join with inner fetches for Lazy loading entities, to avoid multiple joins on the same table.
   *
   * @param from {@link Root}
   * @param attribute {@link PluralAttribute}
   * @param <V> parent class of join
   * @param <T> child class of join
   */
  public static <V, T> Join<V, T> getOrCreateJoinFetch(Root<V> from, Attribute attribute) {
    return (Join<V, T>) from.getFetches()
        .stream()
        .filter(fetch -> attribute.getName().equals(fetch.getAttribute().getName()))
        .findFirst()
        .orElseGet(() -> from.fetch(attribute.getName()));
  }
  
  /**
   * Method to determine whether a query is being issued to identify the count
   * value of a paged response.
   * 
   * @param criteriaQuery {@link CriteriaQuery}
   * @return boolean value for whether a query is to identify the count value of
   *         a paged response.
   */
  public static boolean currentQueryIsCountRecords(
      CriteriaQuery<?> criteriaQuery) {
    return criteriaQuery.getResultType() == Long.class
        || criteriaQuery.getResultType() == long.class;
  }
}
