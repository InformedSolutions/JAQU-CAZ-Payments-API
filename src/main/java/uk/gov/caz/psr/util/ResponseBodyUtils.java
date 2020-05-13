package uk.gov.caz.psr.util;

import java.io.IOException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;

/**
 * Utility class that deals with reading contents of {@link ResponseBody}.
 */
@UtilityClass
@Slf4j
public class ResponseBodyUtils {

  /**
   * Quietly reads the contents of the passed {@code responseBody}. If {@link IOException} is thrown
   * upon the read attempt or {@code responseBody} is {@code null}, an empty string is returned.
   */
  public static String readQuietly(ResponseBody responseBody) {
    try {
      if (responseBody == null) {
        return "";
      }
      return responseBody.string();
    } catch (IOException e) {
      log.error("Error while obtaining response body, returning an empty string", e);
      return "";
    }
  }
}
