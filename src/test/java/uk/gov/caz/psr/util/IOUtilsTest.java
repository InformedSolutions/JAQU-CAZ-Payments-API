package uk.gov.caz.psr.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IOUtilsTest {

  @Test
  public void shouldReturnEmptyStringUponIOException() throws IOException {
    // given
    ResponseBody responseBody = mock(ResponseBody.class);
    when(responseBody.string()).thenThrow(IOException.class);

    // when
    String result = ResponseBodyUtils.readQuietly(responseBody);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  public void shouldReturnEmptyStringWhenPassedInputIsNull() {
    // given
    ResponseBody responseBody = null;

    // when
    String result = ResponseBodyUtils.readQuietly(responseBody);

    // then
    assertThat(result).isEmpty();
  }
}