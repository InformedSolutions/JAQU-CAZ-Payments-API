package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ResourceUtils;
import uk.gov.caz.definitions.dto.accounts.ChargeableVehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges.VehicleCharge;
import uk.gov.caz.psr.model.ChargeableVehicle;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.service.exception.ChargeableAccountVehicleNotFoundException;
import uk.gov.caz.psr.util.TestObjectFactory.EntrantPayments;

@ExtendWith(MockitoExtension.class)
class ChargeableVehiclesServiceTest {

  private final static UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private final static UUID ANY_CAZ_ID = UUID.fromString("5cd7441d-766f-48ff-b8ad-1809586fea37");
  private final static String ANY_VRN = "ANY123";

  @Mock
  private AccountService accountService;

  @InjectMocks
  private ChargeableVehiclesService chargeableVehiclesService;

  @Nested
  class RetrieveOne {

    @Test
    public void shouldThrowChargeableAccountVehicleNotFoundExceptionWhenVehicleIsNotChargeable() {
      // given
      mockMissingVehicleCharge();
      assertExceptionThrowOnServiceCall();
    }

    @Test
    public void shouldThrowChargeableVehicleNotFoundExceptionWhenVehicleHasNoCharges() {
      mockMissingChargeInVehicleCharge();
      assertExceptionThrowOnServiceCall();
    }

    @Test
    public void shouldThrowChargeableVehicleNotFoundExceptionWhenVehicleHasChargesEqualToZero() {
      mockZeroChargeInVehicleCharge();
      assertExceptionThrowOnServiceCall();
    }

    @Test
    public void shouldReturnChargeableVehicleWhenVehicleIsChargeableInCaz() {
      // given
      mockChargeableVehicle();
      mockAccountServicePaidEntrantPaymentsCall();

      // when
      ChargeableVehicle chargeableVehicle = chargeableVehiclesService
          .retrieveOne(ANY_ACCOUNT_ID, ANY_VRN, ANY_CAZ_ID);

      // then
      assertThat(chargeableVehicle).isNotNull();
    }

    private void assertExceptionThrowOnServiceCall() {
      // when
      Throwable throwable = catchThrowable(
          () -> chargeableVehiclesService.retrieveOne(ANY_ACCOUNT_ID, ANY_VRN, ANY_CAZ_ID));

      // then
      assertThat(throwable).isInstanceOf(ChargeableAccountVehicleNotFoundException.class)
          .hasMessage("No chargeable account vehicle found for requested zone");
    }

    private void mockMissingVehicleCharge() {
      VehicleWithCharges vehicleWithCharges = VehicleWithCharges.builder()
          .vrn(ANY_VRN)
          .cachedCharges(Collections.emptyList())
          .build();
      when(accountService.retrieveSingleAccountVehicle(any(), anyString()))
          .thenReturn(vehicleWithCharges);
    }

    private void mockZeroChargeInVehicleCharge() {
      VehicleWithCharges vehicleWithCharges = VehicleWithCharges.builder()
          .vrn(ANY_VRN)
          .cachedCharges(Arrays.asList(VehicleCharge.builder().cazId(ANY_CAZ_ID).build()))
          .build();
      when(accountService.retrieveSingleAccountVehicle(any(), anyString()))
          .thenReturn(vehicleWithCharges);
    }

    private void mockMissingChargeInVehicleCharge() {
      VehicleWithCharges vehicleWithCharges = VehicleWithCharges.builder()
          .vrn(ANY_VRN)
          .cachedCharges(Arrays
              .asList(VehicleCharge.builder().charge(BigDecimal.ZERO).cazId(ANY_CAZ_ID).build()))
          .build();
      when(accountService.retrieveSingleAccountVehicle(any(), anyString()))
          .thenReturn(vehicleWithCharges);
    }

    private void mockChargeableVehicle() {
      VehicleCharge vehicleCharge = VehicleCharge.builder()
          .cazId(ANY_CAZ_ID)
          .charge(BigDecimal.TEN)
          .tariffCode("tariff-code")
          .build();
      VehicleWithCharges vehicleWithCharges = VehicleWithCharges.builder()
          .vrn(ANY_VRN)
          .cachedCharges(Arrays.asList(vehicleCharge))
          .build();
      when(accountService.retrieveSingleAccountVehicle(any(), anyString()))
          .thenReturn(vehicleWithCharges);
    }
  }

  private void mockAccountServicePaidEntrantPaymentsCall() {
    EntrantPayment validEntrantPayment = EntrantPayments.anyPaid().toBuilder()
        .travelDate(LocalDate.of(2020, 1, 1)).build();

    Map<String, List<EntrantPayment>> result =
        ImmutableMap.of("CAS123", Arrays.asList(validEntrantPayment));

    when(accountService.getPaidEntrantPayments(any(), any())).thenReturn(result);
  }

  private ChargeableVehiclesResponseDto exampleNonChargeableVehiclesResponseDto() {
    return ChargeableVehiclesResponseDto.builder()
        .totalVehiclesCount(0)
        .vehicles(Collections.EMPTY_LIST)
        .build();
  }

  @SneakyThrows
  public static String readJson(String file) {
    return new String(
        Files.readAllBytes(ResourceUtils.getFile("classpath:data/json/response/" + file).toPath()));
  }
}