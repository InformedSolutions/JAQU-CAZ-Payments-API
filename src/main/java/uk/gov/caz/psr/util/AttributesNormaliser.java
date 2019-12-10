package uk.gov.caz.psr.util;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

/**
 * A utility class that is responsible for normalising input parameters.
 */
@UtilityClass
public class AttributesNormaliser {

  /**
   * Normalizes passed vrn: removes all whitespaces and makes it uppercase if not null. Returns null
   * otherwise.
   *
   * @param vrn A nullable String which is to be normalised.
   * @return A normalised VRN if not null, null otherwise.
   */
  public static String normalizeVrn(String vrn) {
    if (vrn == null) {
      return null;
    }
    return StringUtils.trimAllWhitespace(vrn).toUpperCase();
  }
}
