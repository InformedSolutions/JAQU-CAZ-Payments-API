package uk.gov.caz.psr;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import org.springframework.util.ResourceUtils;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;

public class ExternalCallsIT {

  private static ClientAndServer vccsMockServer;
  private static ClientAndServer accountsMockServer;
  private static ObjectMapper objectMapper = new ObjectMapper();
  protected static List<String> chargeableVrns = new ArrayList<>();

  @BeforeAll
  public static void startVccsMockServer() {
    vccsMockServer = startClientAndServer(1090);
    accountsMockServer = startClientAndServer(1091);
  }

  @AfterAll
  public static void stopVccsMockServer() {
    vccsMockServer.stop();
    accountsMockServer.stop();
  }

  @AfterEach
  public void resetMockServers() {
    vccsMockServer.reset();
    accountsMockServer.reset();
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

  public void mockVccsBulkComplianceCall(List<String> vrns, String cleanAirZoneId, String filePath,
      int statusCode) throws JsonProcessingException {
    vccsMockServer
        .when(requestPost("/v1/compliance-checker/vehicles/bulk-compliance"),
            exactly(1))
        .respond(bulkComplianceResponseWithVrnAndCleanAirZoneId(filePath, vrns,
            cleanAirZoneId, statusCode));
  }

  public void mockVccsBulkComplianceCallWithMixedResults(List<String> vrns, String cleanAirZoneId,
      List<String> filePaths, int statusCode) throws JsonProcessingException {
    vccsMockServer
        .when(requestPost("/v1/compliance-checker/vehicles/bulk-compliance"),
            exactly(1))
        .respond(bulkComplianceResponseWithVrnAndCleanAirZoneId(filePaths, vrns, cleanAirZoneId,
            statusCode));
  }

  public void mockVccsBulkComplianceCallWithUnknownVrn(List<String> vrns,
      String cleanAirZoneId, String filePath, int statusCode) throws JsonProcessingException {
    vccsMockServer
        .when(requestPost("/v1/compliance-checker/vehicles/bulk-compliance"),
            exactly(1))
        .respond(fullResponseWithSingleUnknown(filePath, vrns, cleanAirZoneId, statusCode));
  }

  public void mockVccsBulkComplianceCallWithError(int statusCode) throws JsonProcessingException {
    vccsMockServer
        .when(requestPost("/v1/compliance-checker/vehicles/bulk-compliance"),
            exactly(1))
        .respond(emptyResponse(statusCode));
  }

  public void mockVccsUnknownVehicleComplianceCall(String type, String cleanAirZoneId,
      String filePath, int statusCode) {
    vccsMockServer
        .when(requestGet("/v1/compliance-checker/vehicles/unrecognised/" + type + "/compliance"),
            exactly(1))
        .respond(responseWithUnrecognisedCompliance(filePath, cleanAirZoneId, statusCode));
  }

  public void mockVccsRegisterDetailsCall(String vrn, String filePath, int statusCode) {
    vccsMockServer
        .when(requestGet("/v1/compliance-checker/vehicles/" + vrn + "/register-details"),
            exactly(1))
        .respond(responseWithRegisterDetails(filePath, statusCode));
  }

  public void mockVccsComplianceCall(String vrn, String cleanAirZoneId, String filePath,
      int statusCode) throws JsonProcessingException {
    vccsMockServer
        .when(requestGet("/v1/compliance-checker/vehicles/" + vrn + "/compliance"),
            exactly(1))
        .respond(responseWithVrnAndCleanAirZoneId(filePath, vrn, cleanAirZoneId, statusCode));
  }

  public void mockVccsVehicleDetailsCall() {
    vccsMockServer
        .when(HttpRequest.request()
            .withPath("/v1/compliance-checker/vehicles/TESTVRN/details")
            .withMethod("GET"))
        .respond(HttpResponse.response()
            .withStatusCode(200)
            .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
            .withBody(readFile("vehicle-details.json")));
  }

  public void mockVccsBulkComplianceCallError(String vrn, int statusCode) {
    vccsMockServer
        .when(requestGet("/v1/compliance-checker/vehicles/bulk-compliance"),
            exactly(1))
        .respond(emptyResponse(statusCode));
  }

  public void mockAccountServiceOffsetCall(String accountId, String vrn) {
    accountsMockServer
        .when(requestGet("/v1/accounts/" + accountId + "/vehicles"),
            exactly(1))
        .respond(responseWithVrn("account-vehicles-response.json", vrn, 200));
  }

  public void mockAccountServiceOffsetCallWithEmptyResponse(String accountId) {
    accountsMockServer
        .when(requestGet("/v1/accounts/" + accountId + "/vehicles"),
            exactly(1))
        .respond(response("account-vehicles-empty-response.json", 200));
  }

  public void mockAccountServiceOffsetCallWithError(String accountId, int statusCode) {
    accountsMockServer
        .when(requestGet("/v1/accounts/" + accountId + "/vehicles"),
            exactly(1))
        .respond(emptyResponse(statusCode));
  }

  public void mockAccountServiceCursorCall(String accountId, String cursor,
      String responseFile) {
    Parameter parameter = new Parameter("vrn", cursor);
    accountsMockServer
        .when(requestGetWithQueryString("/v1/accounts/" + accountId + "/vehicles/sorted-page",
            parameter))
        .respond(response(responseFile, 200));
  }

  public void mockAccountServiceCursorCallWithoutCursorParameter(String accountId,
      String responseFile) {
    accountsMockServer
        .when(requestGet("/v1/accounts/" + accountId + "/vehicles/sorted-page"), exactly(1))
        .respond(response(responseFile, 200));
  }

  public void mockAccountServiceCursorCallWithError(String accountId, String cursor,
      int statusCode) {
    Parameter parameter = new Parameter("vrn", cursor);
    accountsMockServer
        .when(requestGetWithQueryString("/v1/accounts/" + accountId + "/vehicles/sorted-page",
            parameter))
        .respond(emptyResponse(statusCode));
  }

  public void mockAccountServiceChargesSingleVrnCall(String accountId, String vrn, int statusCode) {
    accountsMockServer
        .when(requestGet("/v1/accounts/" + accountId + "/vehicles/" + vrn),
            exactly(1))
        .respond(responseWithVrnAndAccountId("single-account-vehicle-response.json", vrn, accountId,
            statusCode));
  }

  public void mockAccountServiceChargesSingleVrnCallWithError(String accountId, String vrn,
      int statusCode) {
    accountsMockServer
        .when(requestGet("/v1/accounts/" + accountId + "/vehicles/" + vrn),
            exactly(1))
        .respond(emptyResponse(statusCode));
  }

  public void mockAccountServiceGetAllUsersCall(String accountId, int statusCode) {
    accountsMockServer
        .when(requestGet("/v1/accounts/" + accountId + "/users"),
            exactly(1))
        .respond(response("account-users-list-response.json", statusCode));
  }

  public void mockAccountServiceGetUserDetailsCall(String userId, int statusCode) {
    accountsMockServer
        .when(requestGet("/v1/users/"+ userId),
            exactly(1))
        .respond(response("account-user-details.json", statusCode));
  }

  public void mockAccountServiceGetUserDetailsOwnerCall(String userId, int statusCode) {
    accountsMockServer
        .when(requestGet("/v1/users/"+ userId),
            exactly(1))
        .respond(response("account-user-details-owner.json", statusCode));
  }

  public void mockAccountServiceGetUserDetailsRemovedCall(String userId, int statusCode) {
    accountsMockServer
        .when(requestGet("/v1/users/"+ userId),
            exactly(1))
        .respond(response("account-user-details-removed.json", statusCode));
  }

  public void mockAccountServiceGetUserDetailsError(String userId, int statusCode) {
    accountsMockServer
        .when(requestGet("/v1/users/" + userId),
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

  public static HttpResponse response(String responseFile, int statusCode) {
    return HttpResponse.response()
        .withStatusCode(statusCode)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(readJson(responseFile));
  }

  public static HttpResponse responseWithVrn(String responseFile, String vrn, int statusCode) {
    return HttpResponse.response()
        .withStatusCode(statusCode)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(readJson(responseFile).replace("TEST_VRN", vrn));
  }

  public static HttpResponse responseWithVrnAndCleanAirZoneId(String filePath, String vrn,
      String cleanAirZoneId, int statusCode) {
    return HttpResponse.response()
        .withStatusCode(statusCode)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(
            readJson(filePath).replace("TEST_VRN", vrn).replace("TEST_CAZ_ID", cleanAirZoneId));
  }

  public static HttpResponse bulkComplianceResponseWithVrnAndCleanAirZoneId(List<String> filePaths,
      List<String> vrns, String cleanAirZoneId, int statusCode) throws JsonProcessingException {
    List<ComplianceResultsDto> responses = new ArrayList<>();
    int fileNum = 0;
    for (String vrn : vrns) {
      responses.add(getBody(filePaths.get(fileNum), vrn, cleanAirZoneId));
      if (fileNum == 0) {
        chargeableVrns.add(vrn);
      }
      fileNum = 1 - fileNum;
    }
    return HttpResponse.response()
        .withStatusCode(statusCode)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(objectMapper.writeValueAsString(responses));
  }

  public static HttpResponse bulkComplianceResponseWithVrnAndCleanAirZoneId(String filePath,
      List<String> vrns, String cleanAirZoneId, int statusCode) throws JsonProcessingException {
    List<ComplianceResultsDto> responses = new ArrayList<>();
    for (String vrn : vrns) {
      responses.add(getBody(filePath, vrn, cleanAirZoneId));
      chargeableVrns.add(vrn);
    }
    return HttpResponse.response()
        .withStatusCode(statusCode)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(objectMapper.writeValueAsString(responses));
  }

  public static HttpResponse responseWithUnrecognisedCompliance(String filePath,
      String cleanAirZoneId, int statusCode) {
    return HttpResponse.response()
        .withStatusCode(statusCode)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(readJson(filePath)
            .replace("TEST_CAZ_ID", cleanAirZoneId));
  }

  public static HttpResponse responseWithRegisterDetails(String filePath, int statusCode) {
    return HttpResponse.response()
        .withStatusCode(statusCode)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(readJson(filePath));
  }

  public static HttpResponse responseWithVrnAndAccountId(String responseFile, String vrn,
      String accountId, int statusCode) {
    return HttpResponse.response()
        .withStatusCode(statusCode)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(
            readJson(responseFile).replace("TEST_VRN", vrn).replace("TEST_ACCOUNT", accountId));
  }

  private HttpResponse fullResponseWithSingleUnknown(String filePath, List<String> vrns,
      String cleanAirZoneId, int statusCode) throws JsonProcessingException {
    List<ComplianceResultsDto> responses = new ArrayList<>();
    for (int i = 0; i < vrns.size() - 1; i++) {
      responses.add(getBody(filePath, vrns.get(i), cleanAirZoneId));
    }
    responses.add(getBody("vehicle-compliance-null-response.json", vrns.get(vrns.size() - 1),
        cleanAirZoneId));
    return HttpResponse.response()
        .withStatusCode(statusCode)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(objectMapper.writeValueAsString(responses));
  }

  public static HttpResponse emptyResponse(int statusCode) {
    return HttpResponse.response()
        .withStatusCode(statusCode);
  }

  public static HttpRequest requestGet(String url) {
    return prepareRequest(url, "GET");
  }

  public static HttpRequest requestPost(String url) {
    return prepareRequest(url, "POST");
  }

  public static HttpRequest prepareRequest(String url, String method) {
    return HttpRequest.request()
        .withPath(url)
        .withMethod(method);
  }

  private static HttpRequest requestGetWithQueryString(String url,
      Parameter parameter) {
    return HttpRequest.request()
        .withPath(url)
        .withQueryStringParameter(parameter)
        .withMethod("GET");
  }

  private static ComplianceResultsDto getBody(String filePath, String vrn, String cleanAirZoneId)
      throws JsonProcessingException {
    return objectMapper.readValue(readJson(filePath).replace("TEST_VRN", vrn)
        .replace("TEST_CAZ_ID", cleanAirZoneId), ComplianceResultsDto.class);
  }
}
