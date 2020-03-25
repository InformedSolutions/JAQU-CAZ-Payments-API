package uk.gov.caz.psr.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IOUtilsTest {

  @Test
  public void shouldReturnEmptyStringUponIOException() throws IOException {
    ResponseBody responseBody = mock(ResponseBody.class);
    BufferedSource bufferedSource = mock(BufferedSource.class);
    when(responseBody.source()).thenReturn(bufferedSource);
    when(bufferedSource.select(any())).thenReturn(0);
    when(bufferedSource.readString(any())).thenThrow(IOException.class);

    String result = ResponseBodyUtils.readQuietly(responseBody);

    assertThat(result).isEmpty();
  }

  @Test
  public void shouldReturnEmptyStringWhenPassedInputIsNull() {
    ResponseBody responseBody = null;

    String result = ResponseBodyUtils.readQuietly(responseBody);

    assertThat(result).isEmpty();
  }
}