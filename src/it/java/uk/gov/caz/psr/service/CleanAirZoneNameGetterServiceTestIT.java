package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.repository.exception.CleanAirZoneNotFoundException;

@IntegrationTest
public class CleanAirZoneNameGetterServiceTestIT {

  @Autowired
  private CleanAirZoneNameGetterService cleanAirZoneNameGetterService;

  @BeforeEach
  public void startMockServer() {
    mockServer = startClientAndServer(1090);
  }

  @AfterEach
  public void clear() {
    mockServer.stop();
  }

  private ClientAndServer mockServer;

  @Test
  public void
  shouldReturnCleanAirZoneName() {
    // given
    mockVccsResponse();
    UUID cleanAirZoneId = UUID.fromString("53e03a28-0627-11ea-9511-ffaaee87e375");

    // when
    String foundName = cleanAirZoneNameGetterService
        .fetch(cleanAirZoneId);

    // then
    assertThat(foundName).isEqualTo("Birmingham");
  }

  @Test
  public void shouldThrowExceptionIfCleanAirZoneNotFound() {
    // given
    mockVccsResponse();
    UUID cleanAirZoneId = UUID.randomUUID();

    // when
    Throwable throwable = catchThrowable(
        () -> cleanAirZoneNameGetterService.fetch(cleanAirZoneId));

    // then
    assertThat(throwable).isInstanceOf(CleanAirZoneNotFoundException.class)
        .hasMessage("Clean Air Zone not found in VCCS");
  }

  public void mockVccsResponse() {
    mockServer
        .when(HttpRequest.request()
            .withPath("/v1/compliance-checker/clean-air-zones")
            .withMethod("GET"))
        .respond(HttpResponse.response()
            .withStatusCode(200)
            .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
            .withBody(readFile("get-clean-air-zones.json")));
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/" + filename), Charsets.UTF_8);
  }
}
