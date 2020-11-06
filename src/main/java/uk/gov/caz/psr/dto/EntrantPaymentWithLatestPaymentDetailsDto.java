package uk.gov.caz.psr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;

/**
 * A value object that stores some subset of data from single {@link EntrantPayment} and potential
 * matching latest {@link Payment} related to this entrant payment.
 */
@Value
@Builder
public class EntrantPaymentWithLatestPaymentDetailsDto {

  /**
   * Helper static object that converts {@link PaymentMethod} enum to DTOs required String
   * representation.
   */
  private static final PaymentMethodToDtoString paymentMethodToDtoString =
      new PaymentMethodToDtoString();

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.vehicle-entrant-id")
  @NotNull
  UUID entrantPaymentId;

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.status}")
  @NotNull
  String paymentStatus;

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.payment-method}")
  @NotNull
  String paymentMethod;

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.vrn}")
  @NotNull
  String vrn;

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.travel-timestamp}")
  @NotNull
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  LocalDateTime cazEntryTimestamp;

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.clean-zone-id}")
  @NotNull
  UUID cleanAirZoneId;

  @ApiModelProperty(value = "${swagger.model.descriptions.caz-entrant-payment.tariff-code}")
  @Nullable
  String tariffCode;

  /**
   * Maps {@link EntrantPayment} and possibly from {@link Payment} to {@link
   * EntrantPaymentWithLatestPaymentDetailsDto}.
   *
   * @param entrantPayment {@link EntrantPayment} record to be mapped
   * @param payment {@link Payment} entity with latest Payment details matching entrant payment.
   *     Can be null if not yet paid.
   * @return mapped object
   */
  public static EntrantPaymentWithLatestPaymentDetailsDto from(EntrantPayment entrantPayment,
      @Nullable Payment payment) {
    return EntrantPaymentWithLatestPaymentDetailsDto.builder()
        .entrantPaymentId(entrantPayment.getCleanAirZoneEntrantPaymentId())
        .paymentStatus(entrantPayment.getInternalPaymentStatus().toString())
        .vrn(entrantPayment.getVrn())
        .cazEntryTimestamp(entrantPayment.getCazEntryTimestamp())
        .cleanAirZoneId(entrantPayment.getCleanAirZoneId())
        .paymentMethod(
            payment != null ? payment.getPaymentMethod().accept(paymentMethodToDtoString) : "null")
        .tariffCode(entrantPayment.getTariffCode())
        .build();
  }

  /**
   * Maps {@link EntrantPayment} to {@link EntrantPaymentWithLatestPaymentDetailsDto}.
   *
   * @param entrantPayment record to be mapped
   * @return mapped object
   */
  public static EntrantPaymentWithLatestPaymentDetailsDto fromEntrantPaymentOnly(
      EntrantPayment entrantPayment) {
    return from(entrantPayment, null);
  }

  /**
   * Helper class that converts {@link PaymentMethod} enum to DTOs required String representation.
   */
  private static class PaymentMethodToDtoString implements
      PaymentMethod.PaymentMethodVisitor<String> {

    @Override
    public String visitCreditDebitCard() {
      return "card";
    }

    @Override
    public String visitDirectDebit() {
      return "direct_debit";
    }
  }
}
