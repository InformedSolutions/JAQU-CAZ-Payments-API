package uk.gov.caz.psr.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.gov.caz.psr.dto.ChargeSettlementPaymentStatus;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.dto.PaymentStatusUpdateDetails;
import uk.gov.caz.psr.model.CazEntrantPayment;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.model.VehicleEntrantPaymentStatusUpdate;

public class TestObjectFactory {

  private static final Random random = new Random();
  private static final UUID ANY_CLEAN_AIR_ZONE =
      UUID.fromString("17e8e064-fcb9-11e9-995d-fb4ae3c787c6");

  private static String randomVrn() {
    char[] value = {randomUppercase(), randomUppercase(), (char) (random.nextInt(10) + '0'),
        (char) (random.nextInt(10) + '0'), randomUppercase(), randomUppercase(), randomUppercase()};
    return new String(value);
  }

  private static String randomExternalId() {
    char[] value = {(char) (random.nextInt(10) + '0'), randomLowercase(), randomUppercase(),
        randomUppercase(), (char) (random.nextInt(10) + '0'), randomUppercase(), randomLowercase()};
    return new String(value);
  }

  private static char randomUppercase() {
    return (char) (random.nextInt(26) + (int) ('A'));
  }

  private static char randomLowercase() {
    return (char) (random.nextInt(26) + (int) ('a'));
  }

  private static class CazEntrantPaymentsBuilder {

    private final Collection<LocalDate> travelDates;
    private InternalPaymentStatus status;
    private int amount;
    private boolean withId;
    private String vrn;
    private UUID cleanAirZoneId;

    private CazEntrantPaymentsBuilder(Collection<LocalDate> travelDates) {
      this.travelDates = travelDates;
    }

    public static CazEntrantPaymentsBuilder forDays(Collection<LocalDate> days) {
      return new CazEntrantPaymentsBuilder(days);
    }

    public CazEntrantPaymentsBuilder withStatus(InternalPaymentStatus status) {
      this.status = status;
      return this;
    }

    public List<CazEntrantPayment> build() {
      int charge = amount / travelDates.size();
      return travelDates.stream()
          .map(travelDate -> CazEntrantPayment.builder().cleanAirZoneEntrantPaymentId(withId ? UUID.randomUUID() : null)
              .internalPaymentStatus(status)
              .charge(charge)
              .travelDate(travelDate)
              .tariffCode("TARIFF_CODE")
              .cleanAirZoneId(cleanAirZoneId == null ? ANY_CLEAN_AIR_ZONE : cleanAirZoneId)
              .vrn(vrn == null ? randomVrn() : vrn).caseReference(null).build())
          .collect(Collectors.toList());
    }

    public CazEntrantPaymentsBuilder withTotal(Integer amount) {
      this.amount = amount;
      return this;
    }

    public CazEntrantPaymentsBuilder withVrn(String vrn) {
      this.vrn = vrn;
      return this;
    }

    public CazEntrantPaymentsBuilder withCazId(UUID cleanAirZoneId) {
      this.cleanAirZoneId = cleanAirZoneId;
      return this;
    }
  }

  public static class Payments {

    private static final Random random = new Random();

    public static Payment existing() {
      UUID paymentId = UUID.randomUUID();
      String externalId = randomExternalId();
      return forRandomDaysWithId(paymentId, externalId, null);
    }

    public static Payment forRandomDays() {
      return forRandomDaysWithId(null, null, null);
    }

    public static Payment forRandomDaysWithId(UUID paymentId, UUID cazIdentifier) {
      return forRandomDaysWithId(paymentId, null, cazIdentifier);
    }

    public static Payment forRandomDaysWithId(UUID paymentId, String externalId,
        UUID cazIdentifier) {
      int daysSize = 5;
      LocalDate today = LocalDate.now();
      Set<LocalDate> localDates = new HashSet<>();
      while (localDates.size() != daysSize) {
        localDates.add(today.plusDays(random.nextInt(7)));
      }
      return forDays(localDates, paymentId, externalId, cazIdentifier);
    }

    public static Payment forDays(Collection<LocalDate> travelDates, UUID paymentId) {
      return forDays(travelDates, paymentId, null, null);
    }

    public static Payment forDays(Collection<LocalDate> travelDates, UUID paymentId,
        String externalId, UUID cazIdentifier) {
        List<CazEntrantPayment> cazEntrantPayments = CazEntrantPaymentsBuilder.forDays(travelDates).withTotal(travelDates.size() * 800)
            .withVrn(randomVrn())
            .withStatus(InternalPaymentStatus.NOT_PAID).withCazId(cazIdentifier).build();
      return createPaymentWith(cazEntrantPayments, paymentId, externalId);
    }

    public static Payment forRequest(InitiatePaymentRequest request) {
      List<CazEntrantPayment> cazEntrantPayments = CazEntrantPaymentsBuilder
          .forDays(request.getDays()).withTotal(request.getAmount())
          .withVrn(request.getVrn()).withStatus(InternalPaymentStatus.NOT_PAID)
          .withCazId(request.getCleanAirZoneId()).build();

      return createPaymentWith(cazEntrantPayments, null, null);
    }

    private static Payment createPaymentWith(List<CazEntrantPayment> cazEntrantPayments,
        UUID paymentId, String externalId) {
      return Payment.builder().id(paymentId).externalId(externalId)
          .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD)
          .totalPaid(cazEntrantPayments.stream().map(CazEntrantPayment::getCharge)
              .reduce(0, Integer::sum))
          .cazEntrantPayments(cazEntrantPayments)
          .externalPaymentStatus(ExternalPaymentStatus.INITIATED).build();
    }
  }

  public static class CazEntrantPayments {

    public static CazEntrantPayment anyNotPaid() {
      return CazEntrantPayment.builder().charge(100).travelDate(LocalDate.now())
          .cleanAirZoneId(UUID.randomUUID()).vrn("VRN123")
          .internalPaymentStatus(InternalPaymentStatus.NOT_PAID).build();
    }

    public static CazEntrantPayment anyPaid() {
      return CazEntrantPayment.builder().charge(100).travelDate(LocalDate.now())
          .cleanAirZoneId(UUID.randomUUID()).vrn("VRN123")
          .internalPaymentStatus(InternalPaymentStatus.PAID).build();
    }

    public static List<CazEntrantPayment> forRandomDays() {
      int daysSize = 5;
      LocalDate today = LocalDate.now();
      Set<LocalDate> localDates = new HashSet<>();
      while (localDates.size() != daysSize) {
        localDates.add(today.plusDays(random.nextInt(7)));
      }

      return CazEntrantPaymentsBuilder.forDays(localDates)
          .withStatus(InternalPaymentStatus.PAID).build();
    }
  }

  public static class VehicleEntrants {

    public static VehicleEntrant forDay(LocalDate entryDate) {
      return basicBuilder().cazEntryDate(entryDate).cazEntryTimestamp(entryDate.atStartOfDay())
          .build();
    }

    public static VehicleEntrant anyWithoutId() {
      return basicBuilder().build();
    }

    public static VehicleEntrant anyWithId() {
      return basicBuilder().id(UUID.randomUUID()).build();
    }

    public static VehicleEntrant sampleEntrantWithId(UUID uuid) {
      return basicBuilder().id(uuid).build();
    }

    private static VehicleEntrant.VehicleEntrantBuilder basicBuilder() {
      LocalDateTime now = LocalDateTime.now();
      return VehicleEntrant.builder().id(null).vrn("BW91HUN").cazEntryTimestamp(now)
          .cazEntryDate(now.toLocalDate()).cleanZoneId(ANY_CLEAN_AIR_ZONE);
    }
  }

  public static class PaymentStatusUpdateDetailsFactory {

    public static PaymentStatusUpdateDetails anyWithStatus(ChargeSettlementPaymentStatus status) {
      return PaymentStatusUpdateDetails.builder()
          .caseReference("CaseReference")
          .chargeSettlementPaymentStatus(status)
          .dateOfCazEntry(LocalDate.now())
          .paymentProviderId("TestPaymentId")
          .build();
    }

    public static PaymentStatusUpdateDetails refundedWithDateOfCazEntry(LocalDate date) {
      return PaymentStatusUpdateDetails.builder().caseReference("CaseReference")
          .chargeSettlementPaymentStatus(ChargeSettlementPaymentStatus.REFUNDED)
          .dateOfCazEntry(date).build();
    }

    public static PaymentStatusUpdateDetails refundedWithDateOfCazEntryAndPaymentId(LocalDate date,
        String paymentID) {
      return PaymentStatusUpdateDetails.builder().caseReference("CaseReference")
          .chargeSettlementPaymentStatus(ChargeSettlementPaymentStatus.REFUNDED)
          .paymentProviderId(paymentID)
          .dateOfCazEntry(date)
          .build();
    }

    public static PaymentStatusUpdateDetails anyInvalid() {
      return PaymentStatusUpdateDetails.builder()
          .chargeSettlementPaymentStatus(ChargeSettlementPaymentStatus.REFUNDED)
          .paymentProviderId("paymentID")
          .dateOfCazEntry(LocalDate.now())
          .build();
    }

    public static PaymentStatusUpdateDetails withPaymentId(String paymentId) {
      return PaymentStatusUpdateDetails.builder()
          .chargeSettlementPaymentStatus(ChargeSettlementPaymentStatus.REFUNDED)
          .caseReference("caseReference")
          .paymentProviderId(paymentId)
          .dateOfCazEntry(LocalDate.now())
          .build();
    }

    public static PaymentStatusUpdateDetails withCaseReference(String caseReference) {
      return PaymentStatusUpdateDetails.builder()
          .chargeSettlementPaymentStatus(ChargeSettlementPaymentStatus.REFUNDED)
          .caseReference(caseReference)
          .paymentProviderId("paymentID")
          .dateOfCazEntry(LocalDate.now())
          .build();
    }
  }

  public static class VehicleEntrantPaymentStatusUpdates {

    public static VehicleEntrantPaymentStatusUpdate any() {
      return VehicleEntrantPaymentStatusUpdate.builder().caseReference("CaseReference")
          .externalPaymentId("test payment id").paymentStatus(InternalPaymentStatus.REFUNDED)
          .vrn("VRN123").dateOfCazEntry(LocalDate.now()).cleanAirZoneId(UUID.randomUUID()).build();
    }
  }

  public static class PaymentStatusFactory {

    public static PaymentStatus anyWithStatus(InternalPaymentStatus internalPaymentStatus) {
      return PaymentStatus.builder().caseReference("any-valid-case-reference")
          .status(internalPaymentStatus).externalId(UUID.randomUUID().toString()).build();
    }

    public static PaymentStatus with(InternalPaymentStatus internalPaymentStatus,
        String caseReference, String externalId) {
      return PaymentStatus.builder().caseReference(caseReference).status(internalPaymentStatus)
          .externalId(externalId).build();
    }
  }

  public static class ExternalPaymentDetailsFactory {
    public static ExternalPaymentDetails any() {
      return ExternalPaymentDetails.builder().email("example@email.com")
          .externalPaymentStatus(ExternalPaymentStatus.SUCCESS).build();
    }

    public static ExternalPaymentDetails anyWithStatus(
        ExternalPaymentStatus externalPaymentStatus) {
      return ExternalPaymentDetails.builder().email("example@email.com")
          .externalPaymentStatus(externalPaymentStatus).build();
    }
  }

}
