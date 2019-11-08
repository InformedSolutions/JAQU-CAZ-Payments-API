package uk.gov.caz.psr.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.model.VehicleEntrantPayment;

public class TestObjectFactory {

  private static final Random random = new Random();

  private static String randomVrn() {
    char[] value = {
        randomUppercase(),
        randomUppercase(),
        (char) (random.nextInt(10) + '0'),
        (char) (random.nextInt(10) + '0'),
        randomUppercase(),
        randomUppercase(),
        randomUppercase()
    };
    return new String(value);
  }

  private static String randomExternalId() {
    char[] value = {
        (char) (random.nextInt(10) + '0'),
        randomLowercase(),
        randomUppercase(),
        randomUppercase(),
        (char) (random.nextInt(10) + '0'),
        randomUppercase(),
        randomLowercase()
    };
    return new String(value);
  }

  private static char randomUppercase() {
    return (char) (random.nextInt(26) + (int) ('A'));
  }

  private static char randomLowercase() {
    return (char) (random.nextInt(26) + (int) ('a'));
  }

  private static class VehicleEntrantPaymentsBuilder {
    private final Collection<LocalDate> travelDates;
    private UUID paymentId;
    private PaymentStatus status;
    private int amount;
    private boolean withId;
    private String vrn;
    private UUID cleanAirZoneId;

    private VehicleEntrantPaymentsBuilder(Collection<LocalDate> travelDates) {
      this.travelDates = travelDates;
    }

    public static VehicleEntrantPaymentsBuilder forDays(Collection<LocalDate> days) {
      return new VehicleEntrantPaymentsBuilder(days);
    }

    public VehicleEntrantPaymentsBuilder withPaymentId(UUID paymentId) {
      this.paymentId = paymentId;
      return this;
    }

    public VehicleEntrantPaymentsBuilder withStatus(PaymentStatus status) {
      this.status = status;
      return this;
    }

    public List<VehicleEntrantPayment> build() {
      int charge = amount / travelDates.size();
      return travelDates.stream()
          .map(travelDate -> VehicleEntrantPayment.builder()
              .id(withId ? UUID.randomUUID() : null)
              .paymentId(paymentId)
              .status(status)
              .chargePaid(charge)
              .travelDate(travelDate)
              .cleanZoneId(cleanAirZoneId == null
                  ? UUID.fromString("17e8e064-fcb9-11e9-995d-fb4ae3c787c6")
                  : cleanAirZoneId)
              .vrn(vrn == null ? randomVrn() : vrn)
              .caseReference(null)
              .build()
          ).collect(Collectors.toList());
    }

    public VehicleEntrantPaymentsBuilder withTotal(Integer amount) {
      this.amount = amount;
      return this;
    }

    public VehicleEntrantPaymentsBuilder withVrn(String vrn) {
      this.vrn = vrn;
      return this;
    }

    public VehicleEntrantPaymentsBuilder withCazId(UUID cleanAirZoneId) {
      this.cleanAirZoneId = cleanAirZoneId;
      return this;
    }
  }

  public static class Payments {

    private static final Random random = new Random();

    public static Payment existing() {
      UUID paymentId = UUID.randomUUID();
      String externalId = randomExternalId();
      return forRandomDaysWithId(paymentId, externalId);
    }

    public static Payment forRandomDays() {
      return forRandomDaysWithId(null, null);
    }

    public static Payment forRandomDaysWithId(UUID paymentId) {
      return forRandomDaysWithId(paymentId, null);
    }

    public static Payment forRandomDaysWithId(UUID paymentId, String externalId) {
      int daysSize = 5;
      LocalDate today = LocalDate.now();
      Set<LocalDate> localDates = new HashSet<>();
      while(localDates.size() != daysSize) {
        localDates.add(today.plusDays(random.nextInt(7)));
      }
      return forDays(localDates, paymentId, externalId);
    }

    public static Payment forDays(Collection<LocalDate> travelDates, UUID paymentId) {
      return forDays(travelDates, paymentId, null);
    }

    public static Payment forDays(Collection<LocalDate> travelDates, UUID paymentId,
        String externalId) {
      List<VehicleEntrantPayment> vehicleEntrantPayments =
          VehicleEntrantPaymentsBuilder.forDays(travelDates)
              .withTotal(travelDates.size() * 800)
              .withPaymentId(paymentId)
              .withVrn(randomVrn())
              .withStatus(PaymentStatus.INITIATED)
              .build();

      return createPaymentWith(vehicleEntrantPayments, paymentId, externalId);
    }

    public static Payment forRequest(InitiatePaymentRequest request) {
      List<VehicleEntrantPayment> vehicleEntrantPayments =
          VehicleEntrantPaymentsBuilder.forDays(request.getDays())
              .withTotal(request.getAmount())
              .withPaymentId(null)
              .withVrn(request.getVrn())
              .withStatus(PaymentStatus.INITIATED)
              .withCazId(request.getCleanAirZoneId())
              .build();

      return createPaymentWith(vehicleEntrantPayments, null, null);
    }

    private static Payment createPaymentWith(List<VehicleEntrantPayment> vehicleEntrantPayments,
        UUID paymentId, String externalId) {
      return Payment.builder()
          .id(paymentId)
          .externalId(externalId)
          .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD)
          .totalPaid(vehicleEntrantPayments.stream()
              .map(VehicleEntrantPayment::getChargePaid)
              .reduce(0, Integer::sum))
          .vehicleEntrantPayments(vehicleEntrantPayments)
          .build();
    }
  }

  public static class VehicleEntrants {

    public static VehicleEntrant SAMPLE_ENTRANT = VehicleEntrant.builder()
        .vrn("BW91HUN")
        .cazEntryTimestamp(LocalDateTime.now())
        .cleanZoneId(UUID.randomUUID())
        .build();

    public static VehicleEntrant sampleEntrantWithId(UUID uuid) {
      return VehicleEntrant.builder()
          .vrn("BW91HUN")
          .cazEntryTimestamp(LocalDateTime.now())
          .id(UUID.fromString(uuid.toString()))
          .cleanZoneId(UUID.randomUUID())
          .build();
    }
  }
}
