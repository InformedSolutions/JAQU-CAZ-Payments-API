package uk.gov.caz.psr.util;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.JsonBody.json;

import com.google.common.net.MediaType;
import java.nio.file.Files;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.util.ResourceUtils;

public abstract class MockServerTestIT {

  protected static ClientAndServer vccMockServer;
  protected static ClientAndServer accountMockServer;

  protected static final String LEEDS_CAZ = "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3";
  protected static final String BIRMINGHAM_CAZ = "0d7ab5c4-5fff-4935-8c4e-56267c0c9493";

  @BeforeAll
  public static void setupMockServers() {
    vccMockServer = startClientAndServer(1090);
    accountMockServer = startClientAndServer(1091);
  }

  @AfterAll
  public static void cleanUp(){
    vccMockServer.stop();
    accountMockServer.stop();
  }

  @SneakyThrows
  public static String readJson(String file) {
    return new String(
        Files.readAllBytes(ResourceUtils.getFile("classpath:data/json/response/" + file).toPath()));
  }

  public static HttpResponse response(String responseFile) {
    return HttpResponse.response()
        .withStatusCode(200)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(readJson(responseFile));
  }

  public static HttpRequest requestGet(String url) {
    return prepareRequest(url, "GET");
  }

  public static HttpRequest requestPost(String url, String requestFile) {
    return prepareRequest(url, "POST")
        .withBody(json(readJson(requestFile), MediaType.parse("application/json")));
  }

  public static HttpRequest prepareRequest(String url, String method) {
    return HttpRequest.request()
        .withPath(url)
        .withMethod(method);
  }
  
//  protected void whenEachCazHasTariffInfo() {
//    whenEachCazHasTariffInfo(BIRMINGHAM_CAZ, "tariff-rates-first-response.json");
//    whenEachCazHasTariffInfo(LEEDS_CAZ, "tariff-rates-second-response.json");
//  }
//  
//  protected void whenEachCazHasTariffInfo(String cazId, String filename) {
//    mockTariffCall(cazId, filename);
//  }
//
//  protected void whenVehicleIsInTaxiDb(String vrn) {
//    // mockServer.when(requestGet("/v1/vehicles/" + vrn + "/licence-info"))
//    //     .respond(response("ntr-first-response.json"));
//    whenVehicleIsInTaxiDb(vrn,"ntr-first-response.json");
//  }
//  
//  protected void whenVehicleIsInTaxiDb(String vrn, String filename) {
//    mockServer.when(requestGet("/v1/vehicles/" + vrn + "/licence-info"))
//        .respond(response(filename));
//  }
//
//  protected void whenVehicleIsNotInTaxiDb(String vrn) {
//    mockServer.when(requestGet("/v1/vehicles/" + vrn + "/licence-info"))
//        .respond(HttpResponse.response().withStatusCode(404));    
//  }
//
//  protected void whenCazInfoIsInTariffService() {
//    // mockServer.when(requestGet("/v1/clean-air-zones"))
//    //     .respond(response("caz-first-response.json"));
//    whenCazInfoIsInTariffService("/v1/clean-air-zones","caz-first-response.json");
//  }
//
//  protected void whenCazInfoIsInTariffService(String path, String filename) {
//    mockServer.when(requestGet(path))
//        .respond(response(filename));
//  }
//
//  protected void mockTariffCall(String cazId, String file) {
//    mockServer.when(requestGet("/v1/clean-air-zones/" + cazId + "/tariff"))
//        .respond(response(file));
//  }
}