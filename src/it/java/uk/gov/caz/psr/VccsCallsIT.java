package uk.gov.caz.psr;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

public class VccsCallsIT {

  private ClientAndServer vccsMockServer;

  @BeforeEach
  public void startVccsMockServer() {
    vccsMockServer = startClientAndServer(1090);
  }

  @AfterEach
  public void stopVccsMockServer() {
    vccsMockServer.stop();
  }

  public void mockVccsCleanAirZonesCall() {
    vccsMockServer
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
    return Resources.toString(Resources.getResource("data/external/" + filename),
        Charsets.UTF_8);
  }
}
