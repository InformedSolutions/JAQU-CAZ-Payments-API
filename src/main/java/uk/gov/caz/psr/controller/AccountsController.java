package uk.gov.caz.psr.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.controller.util.QueryStringValidator;
import uk.gov.caz.psr.dto.ChargeableAccountVehicleResponse;
import uk.gov.caz.psr.dto.SuccessfulPaymentsResponse;
import uk.gov.caz.psr.model.ChargeableVehicle;
import uk.gov.caz.psr.model.ChargeableVehiclesPage;
import uk.gov.caz.psr.model.EnrichedPaymentSummary;
import uk.gov.caz.psr.model.PaginationData;
import uk.gov.caz.psr.service.ChargeableVehiclesService;
import uk.gov.caz.psr.service.RetrieveSuccessfulPaymentsService;
import uk.gov.caz.psr.util.ChargeableVehicleToDtoConverter;
import uk.gov.caz.psr.util.ChargeableVehiclesToDtoConverter;

@AllArgsConstructor
@RestController
public class AccountsController implements AccountControllerApiSpec {

  public static final String ACCOUNTS_PATH = "/v1/accounts";
  private static final String PAGE_NUMBER_QUERYSTRING_KEY = "pageNumber";
  private static final String PAGE_SIZE_QUERYSTRING_KEY = "pageSize";
  private static final String CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY = "cleanAirZoneId";
  private static final String QUERY_QUERYSTRING_KEY = "query";

  private final ChargeableVehiclesService chargeableVehiclesService;
  private final QueryStringValidator queryStringValidator;
  private final RetrieveSuccessfulPaymentsService retrieveSuccessfulPaymentsService;
  private final ChargeableVehiclesToDtoConverter chargeableVehiclesToDtoConverter;
  private final ChargeableVehicleToDtoConverter chargeableVehicleToDtoConverter;

  @Override
  public ResponseEntity<ChargeableAccountVehicleResponse> retrieveChargeableVehicles(UUID accountId,
      Map<String, String> queryStrings) {

    queryStringValidator.validateRequest(queryStrings,
        Arrays.asList(CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY),
        Arrays.asList(PAGE_SIZE_QUERYSTRING_KEY, PAGE_NUMBER_QUERYSTRING_KEY));

    UUID cleanAirZoneId = UUID.fromString(queryStrings.get(CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY));
    String query = queryStrings.get(QUERY_QUERYSTRING_KEY);
    int pageNumber = Integer.parseInt(queryStrings.get(PAGE_NUMBER_QUERYSTRING_KEY));
    int pageSize = Integer.parseInt(queryStrings.get(PAGE_SIZE_QUERYSTRING_KEY));

    ChargeableVehiclesPage chargeableVehiclesPage = chargeableVehiclesService
        .retrieve(accountId, cleanAirZoneId, query, pageNumber, pageSize);

    return ResponseEntity.ok()
        .body(chargeableVehiclesToDtoConverter
            .toChargeableAccountVehicleResponse(chargeableVehiclesPage));
  }

  @Override
  public ResponseEntity<ChargeableAccountVehicleResponse> retrieveSingleChargeableVehicle(
      UUID accountId, String vrn, Map<String, String> queryStrings) {

    queryStringValidator.validateRequest(queryStrings,
        Arrays.asList(CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY), null);
    UUID cleanAirZoneId = UUID.fromString(queryStrings.get(CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY));

    ChargeableVehicle chargeableVehicle = chargeableVehiclesService
        .retrieveOne(accountId, vrn, cleanAirZoneId);

    return ResponseEntity
        .ok(chargeableVehicleToDtoConverter.toChargeableAccountVehicleResponse(chargeableVehicle));
  }

  @Override
  public ResponseEntity<SuccessfulPaymentsResponse> retrieveSuccessfulPayments(UUID accountId,
      Map<String, String> queryStrings) {
    validateRetrieveSuccessfulPaymentsRequest(queryStrings);

    int pageNumber = Integer.parseInt(queryStrings.get("pageNumber"));
    int pageSize = Integer.parseInt(queryStrings.get("pageSize"));

    Pair<PaginationData, List<EnrichedPaymentSummary>> result;

    if (queryStrings.containsKey("accountUserId")) {
      UUID accountUserId = UUID.fromString(queryStrings.get("accountUserId"));
      result = retrieveSuccessfulPaymentsService
          .retrieveForSingleUser(accountId, accountUserId, pageNumber, pageSize);
    } else {
      result = retrieveSuccessfulPaymentsService
          .retrieveForAccount(accountId, pageNumber, pageSize);
    }

    PaginationData paginationData = result.getFirst();
    List<EnrichedPaymentSummary> payments = result.getSecond();

    return ResponseEntity.ok(SuccessfulPaymentsResponse.from(paginationData, payments));
  }

  private void validateRetrieveSuccessfulPaymentsRequest(Map<String, String> queryStrings) {
    queryStringValidator.validateRequest(queryStrings,
        Collections.emptyList(),
        Arrays.asList(PAGE_NUMBER_QUERYSTRING_KEY, PAGE_SIZE_QUERYSTRING_KEY));
  }
}
