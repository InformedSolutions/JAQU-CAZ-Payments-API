package uk.gov.caz.psr.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.controller.util.QueryStringValidator;
import uk.gov.caz.psr.dto.ChargeableAccountVehicleResponse;
import uk.gov.caz.psr.dto.ChargeableAccountVehiclesResult;
import uk.gov.caz.psr.dto.SuccessfulPaymentsResponse;
import uk.gov.caz.psr.model.ChargeableVehicle;
import uk.gov.caz.psr.model.EnrichedPaymentSummary;
import uk.gov.caz.psr.model.PaginationData;
import uk.gov.caz.psr.service.AccountService;
import uk.gov.caz.psr.service.ChargeableVehiclesService;
import uk.gov.caz.psr.service.RetrieveSuccessfulPaymentsService;
import uk.gov.caz.psr.util.ChargeableVehiclesToDtoConverter;

@AllArgsConstructor
@RestController
public class AccountsController implements AccountControllerApiSpec {

  public static final String ACCOUNTS_PATH = "/v1/accounts";
  private static final String PAGE_NUMBER_QUERYSTRING_KEY = "pageNumber";
  private static final String PAGE_SIZE_QUERYSTRING_KEY = "pageSize";
  private static final String CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY = "cleanAirZoneId";

  private final ChargeableVehiclesService chargeableVehiclesService;
  private final AccountService accountService;
  private final QueryStringValidator queryStringValidator;
  private final RetrieveSuccessfulPaymentsService retrieveSuccessfulPaymentsService;
  private final ChargeableVehiclesToDtoConverter chargeableVehiclesToDtoConverter;

  @Override
  public ResponseEntity<ChargeableAccountVehicleResponse> retrieveChargeableVehicles(UUID accountId,
      Map<String, String> queryStrings) {

    queryStringValidator.validateRequest(queryStrings,
        Arrays.asList(CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY), Arrays.asList(PAGE_SIZE_QUERYSTRING_KEY));

    String vrn = queryStrings.get("vrn");
    UUID cleanAirZoneId = UUID.fromString(queryStrings.get(CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY));
    String direction = queryStrings.get("direction");
    int pageSize = Integer.parseInt(queryStrings.get(PAGE_SIZE_QUERYSTRING_KEY));

    List<ChargeableVehicle> chargeableVehicles = chargeableVehiclesService
        .retrieve(accountId, vrn, cleanAirZoneId, direction, pageSize);

    if (chargeableVehicles.isEmpty()) {
      return ResponseEntity.ok().body(
          ChargeableAccountVehicleResponse.builder().firstVrn(null).lastVrn(null)
              .chargeableAccountVehicles(
                  ChargeableAccountVehiclesResult.builder().results(Collections.emptyList())
                      .build())
              .build());
    }

    return ResponseEntity.ok()
        .body(chargeableVehiclesToDtoConverter
            .toChargeableAccountVehicleResponse(chargeableVehicles, direction, pageSize,
                !StringUtils.hasText(vrn)));
  }

  @Override
  public ResponseEntity<ChargeableAccountVehicleResponse> retrieveSingleChargeableVehicle(
      UUID accountId, String vrn, Map<String, String> queryStrings) {

    queryStringValidator.validateRequest(queryStrings,
        Arrays.asList(CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY), null);
    UUID cleanAirZoneId = UUID.fromString(queryStrings.get(CLEAN_AIR_ZONE_ID_QUERYSTRING_KEY));

    ChargeableAccountVehiclesResult vrnsAndEntryDates = accountService
        .retrieveSingleChargeableAccountVehicle(accountId, vrn, cleanAirZoneId);

    ChargeableAccountVehicleResponse response = ChargeableAccountVehicleResponse
        .builder()
        .chargeableAccountVehicles(vrnsAndEntryDates)
        .build();

    return ResponseEntity.ok()
        .body(response);
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
