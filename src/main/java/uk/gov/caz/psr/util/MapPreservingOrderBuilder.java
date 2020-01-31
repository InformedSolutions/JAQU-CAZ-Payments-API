package uk.gov.caz.psr.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper class that builds special kind of map that preserves order.
 */
public class MapPreservingOrderBuilder<A, B> {
  private final LinkedHashMap<A, B> map;

  /**
   * Internal constructor for MapPreservingOrderBuilder class.
   */
  private MapPreservingOrderBuilder(LinkedHashMap<A, B> map) {
    this.map = map;
  }

  /**
   * Static public constructor that is used to initialize MapPreservingOrderBuilder.
   */
  public static <A, B> MapPreservingOrderBuilder<A, B> builder() {
    return new MapPreservingOrderBuilder<>(new LinkedHashMap<>());
  }

  /**
   * Method that allows to append value into the map.
   */
  public MapPreservingOrderBuilder<A, B> put(A key, B value) {
    map.put(key, value);
    return this;
  }

  /**
   * Method that returns Map that preserves order of elements.
   */
  public Map<A, B> build() {
    return map;
  }
}
