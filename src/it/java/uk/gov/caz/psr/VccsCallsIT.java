package uk.gov.caz.psr;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import java.nio.file.Files;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.util.ResourceUtils;

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
  
  public void mockVccsComplianceCall(String vrn, String responseFile, int statusCode) {
    vccsMockServer
        .when(requestGet("/v1/compliance-checker/vehicles/" + vrn + "/compliance"),
            exactly(1))
        .respond(response(responseFile, vrn, statusCode));
  }
  
  public void mockVccsComplianceCallError(String vrn, int statusCode) {
    vccsMockServer
    .when(requestGet("/v1/compliance-checker/vehicles/" + vrn + "/compliance"),
        exactly(1))
    .respond(emptyResponse(statusCode));
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/" + filename),
        Charsets.UTF_8);
  }
  
  @SneakyThrows
  public static String readJson(String file) {
    return new String(
        Files.readAllBytes(ResourceUtils.getFile("classpath:data/json/response/" + file).toPath()));
  }

  public static HttpResponse response(String responseFile, String vrn, int statusCode) {
    return HttpResponse.response()
        .withStatusCode(statusCode)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(readJson(responseFile).replace("TEST_VRN", vrn));
  }
  
  public static HttpResponse emptyResponse(int statusCode) {
    return HttpResponse.response()
        .withStatusCode(statusCode);
  }

  public static HttpRequest requestGet(String url) {
    return prepareRequest(url, "GET");
  }

  public static HttpRequest prepareRequest(String url, String method) {
    return HttpRequest.request()
        .withPath(url)
        .withMethod(method);
  }
}
