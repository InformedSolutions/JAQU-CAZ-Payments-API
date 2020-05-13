package uk.gov.caz.psr.util;

import com.google.common.base.Preconditions;
import lombok.experimental.UtilityClass;

/**
 * Static utility methods related to {@link String} instances.
 */
@UtilityClass
public class Strings {

  private static final int DEFAULT_TO_REVEAL_LENGTH = 3;

  /**
   * Masks the passed {@code input} with '*' and reveals its first three characters.
   *
   * @param input String which is to be masked.
   * @return A masked string with its first three characters revealed.
   * @throws NullPointerException if {@code input} is null.
   */
  public static String mask(String input) {
    Preconditions.checkNotNull(input, "'input' cannot be null");

    int toReveal = Math.min(DEFAULT_TO_REVEAL_LENGTH, input.length());
    int toMask = input.length() - toReveal;
    return input.substring(0, toReveal) + com.google.common.base.Strings.repeat("*", toMask);
  }
}
