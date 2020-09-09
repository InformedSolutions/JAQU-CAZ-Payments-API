package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ResourceUtils;
import uk.gov.caz.definitions.dto.accounts.ChargeableVehiclesResponseDto;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.psr.model.ChargeableVehicle;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.util.TestObjectFactory.EntrantPayments;

@ExtendWith(MockitoExtension.class)
class ChargeableVehiclesServiceTest {

  private final static UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private final static UUID ANY_CAZ_ID = UUID.fromString("5cd7441d-766f-48ff-b8ad-1809586fea37");
  private final static String ANY_CURSOR_VRN = "ANY123";
  private final static int ANY_PAGE_SIZE = 10;

  @Mock
  private AccountService accountService;

  @InjectMocks
  private ChargeableVehiclesService chargeableVehiclesService;

  @Test
  public void shouldThrowInvalidRequestPayloadExceptionWhenInvalidDirection() {
    // given
    String direction = "any";

    // when
    Throwable throwable =
        catchThrowable(
            () -> chargeableVehiclesService
                .retrieve(ANY_ACCOUNT_ID, ANY_CURSOR_VRN, ANY_CAZ_ID, direction, 1));

    // then
    assertThat(throwable).isInstanceOf(InvalidRequestPayloadException.class)
        .hasMessage("Direction supplied must be one of either 'next' or 'previous'.");
  }

  @Test
  public void shouldThrowInvalidRequestPayloadExceptionWhenPreviousDirectionAndNoCursor() {
    // given
    String cursorVrn = "";
    String direction = "previous";

    // when
    Throwable throwable =
        catchThrowable(
            () -> chargeableVehiclesService
                .retrieve(ANY_ACCOUNT_ID, cursorVrn, ANY_CAZ_ID, direction, 1));

    // then
    assertThat(throwable).isInstanceOf(InvalidRequestPayloadException.class)
        .hasMessage("Direction cannot be set to 'previous' if no VRN has been provided.");
  }

  @Test
  public void shouldNotThrowInvalidRequestPayloadExceptionWhenPreviousDirectionAndPresentCursor()
      throws JsonProcessingException {
    // given
    String direction = "previous";
    mockAccountServiceChargeableVehiclesCall();
    mockAccountServicePaidEntrantPaymentsCall();

    // when
    Throwable throwable =
        catchThrowable(
            () -> chargeableVehiclesService
                .retrieve(ANY_ACCOUNT_ID, ANY_CURSOR_VRN, ANY_CAZ_ID, direction, ANY_PAGE_SIZE));

    // then
    assertThat(throwable).isNull();
  }

  @Test
  public void shouldNotReturnNonChargeableVehiclesFromApi()
      throws JsonProcessingException {
    // given
    String direction = "previous";
    int pageSize = 3;
    mockAccountServiceNonChargeableVehiclesCall(pageSize);
    mockAccountServicePaidEntrantPaymentsCall();

    // when
    List<ChargeableVehicle> response = chargeableVehiclesService
        .retrieve(ANY_ACCOUNT_ID, ANY_CURSOR_VRN, ANY_CAZ_ID, direction, pageSize);

    // then
    assertThat(response.size()).isEqualTo(1);
  }

  private void mockAccountServiceChargeableVehiclesCall() throws JsonProcessingException {
    when(accountService
        .getAccountVehiclesByCursor(ANY_ACCOUNT_ID, "previous", ANY_PAGE_SIZE * 3, ANY_CURSOR_VRN))
        .thenReturn(exampleChargeableVehiclesResponseDto());
  }

  private void mockAccountServicePaidEntrantPaymentsCall() {
    EntrantPayment validEntrantPayment = EntrantPayments.anyPaid().toBuilder()
        .travelDate(LocalDate.of(2020, 1, 1)).build();

    Map<String, List<EntrantPayment>> result =
        ImmutableMap.of("CAS123", Arrays.asList(validEntrantPayment));

    when(accountService.getPaidEntrantPayments(any(), any())).thenReturn(result);
  }

  private ChargeableVehiclesResponseDto exampleChargeableVehiclesResponseDto()
      throws JsonProcessingException {
    return new ObjectMapper()
        .readValue(readJson("account-single-chargeable-vehicles-response.json"),
            ChargeableVehiclesResponseDto.class);
  }

  private void mockAccountServiceNonChargeableVehiclesCall(int pageSize)
      throws JsonProcessingException {
    when(accountService
        .getAccountVehiclesByCursor(ANY_ACCOUNT_ID, "previous", pageSize * 3, ANY_CURSOR_VRN))
        .thenReturn(exampleNonChargeableVehiclesResponseDto());
  }

  private ChargeableVehiclesResponseDto exampleNonChargeableVehiclesResponseDto()
      throws JsonProcessingException {
    return new ObjectMapper()
        .readValue(readJson("account-vehicles-not-chargeable-cursor-response.json"),
            ChargeableVehiclesResponseDto.class);
  }

  @SneakyThrows
  public static String readJson(String file) {
    return new String(
        Files.readAllBytes(ResourceUtils.getFile("classpath:data/json/response/" + file).toPath()));
  }

}