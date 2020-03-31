package uk.gov.caz.psr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.EntrantPaymentWithLatestPaymentDetailsDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.exception.NotUniqueVehicleEntrantPaymentFoundException;
import uk.gov.caz.psr.service.EntrantPaymentService;
import uk.gov.caz.psr.util.TestObjectFactory.EntrantPayments;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ContextConfiguration(classes = {Configuration.class, VehicleEntrantController.class,
    ExceptionController.class})
@WebMvcTest
class VehicleEntrantControllerTest {

  @MockBean
  private EntrantPaymentService entrantPaymentService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";
  private static final UUID ANY_CLEAN_ZONE_ID =
      UUID.fromString("e41d36a6-fcd6-4467-9368-af75201dcae2");
  private static final String ANY_VALID_VRN = "DL76MWX";
  private static final String PATH = VehicleEntrantController.BASE_PATH + "/"
      + VehicleEntrantController.CREATE_VEHICLE_ENTRANT_AND_GET_PAYMENT_DETAILS_PATH;

  @BeforeEach
  public void resetMocks() {
    Mockito.reset(entrantPaymentService);
  }

  @Nested
  class Validation {

    @Test
    public void shouldReturn400StatusCodeWhenCleanZoneIdIsNull() throws Exception {
      String payload = requestWithCleanZoneId(null);

      mockMvc
          .perform(post(PATH).content(payload)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
      // TODO add assertions for a message

      verify(entrantPaymentService, never()).bulkProcess(any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenCazEntryTimestampIsNull() throws Exception {
      String payload = requestWithCazEntryTimestamp(null);

      mockMvc
          .perform(post(PATH).content(payload)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
      // TODO add assertions for a message

      verify(entrantPaymentService, never()).bulkProcess(any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenVrnIsNull() throws Exception {
      String payload = requestWithVrn(null);

      mockMvc
          .perform(post(PATH).content(payload)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
      // TODO add assertions for a message

      verify(entrantPaymentService, never()).bulkProcess(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "aaaaaaaaaaaaaaaa"})
    public void shouldReturn400StatusCodeWhenVrnIsInvalid(String vrn) throws Exception {
      String payload = requestWithVrn(vrn);

      mockMvc
          .perform(post(PATH).content(payload)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
      // TODO add assertions for a message

      verify(entrantPaymentService, never()).bulkProcess(any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenNotUniqueEntrantPaymentFoundExceptionIsThrown()
        throws Exception {
      String payload = requestWithValidBody();
      mockThrowingNotUniqueVehicleEntrantPaymentFoundException();

      mockMvc.perform(post(PATH).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(jsonPath("$.errors[0].title").value("Validation error"))
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrn").value("MY-VRN"))
          .andExpect(
              jsonPath("$.errors[0].detail").value("Not able to find unique EntrantPayment"));
    }

    private void mockThrowingNotUniqueVehicleEntrantPaymentFoundException() {
      when(entrantPaymentService.bulkProcess(anyList()))
          .thenThrow(new NotUniqueVehicleEntrantPaymentFoundException("MY-VRN",
              "Not able to find unique EntrantPayment"));
    }
  }

  @Test
  public void shouldReturn200StatusCodeAndValidResponsePayloadWhenNotPaid() throws Exception {
    String payload = requestWithValidBody();
    mockValidScenarioWithUnpaidRecordFound();

    mockMvc.perform(post(PATH).content(payload)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(jsonPath("$[0].paymentStatus").value(InternalPaymentStatus.NOT_PAID.name()))
        .andExpect(jsonPath("$[0].paymentMethod").value("null"));

    verify(entrantPaymentService).bulkProcess(any());
  }

  @Test
  public void shouldReturn200StatusCodeAndValidResponsePayloadPaidWhenPaidPayment()
      throws Exception {
    String payload = requestWithValidBody();
    mockValidScenarioWithPaidRecordFound();

    mockMvc
        .perform(post(PATH).content(payload)
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(jsonPath("$[0].paymentStatus").value(InternalPaymentStatus.PAID.name()))
        .andExpect(jsonPath("$[0].paymentMethod").value("card"));

    verify(entrantPaymentService).bulkProcess(any());
  }

  @SneakyThrows
  private String toJson(List<VehicleEntrantDto> request) {
    return objectMapper.writeValueAsString(request);
  }

  private LocalDateTime todayDateTime() {
    return LocalDateTime.now();
  }

  private String requestWithCleanZoneId(UUID cleanZoneId) {
    List<VehicleEntrantDto> request = Arrays.asList(
        baseVehicleEntrantDtoBuilder().cleanZoneId(cleanZoneId).build()
    );
    return toJson(request);
  }

  private String requestWithCazEntryTimestamp(LocalDateTime cazEntryTimestamp) {
    List<VehicleEntrantDto> request = Arrays.asList(
        baseVehicleEntrantDtoBuilder().cazEntryTimestamp(cazEntryTimestamp).build()
    );
    return toJson(request);
  }

  private String requestWithVrn(String vrn) {
    List<VehicleEntrantDto> request = Arrays.asList(
        baseVehicleEntrantDtoBuilder().vrn(vrn).build()
    );
    return toJson(request);
  }

  private String requestWithValidBody() {
    List<VehicleEntrantDto> request = Arrays.asList(baseVehicleEntrantDtoBuilder().build());
    return toJson(request);
  }

  private void mockValidScenarioWithPaidRecordFound() {
    EntrantPayment foundVehicleEntrantPayment = EntrantPayments.anyPaid();
    Payment payment = Payments.existing();
    List<EntrantPaymentWithLatestPaymentDetailsDto> listWithPaidCazEntrantPaymentDto = Arrays
        .asList(
            EntrantPaymentWithLatestPaymentDetailsDto.from(foundVehicleEntrantPayment, payment)
        );
    given(entrantPaymentService.bulkProcess(any()))
        .willReturn(listWithPaidCazEntrantPaymentDto);
  }

  private void mockValidScenarioWithUnpaidRecordFound() {
    EntrantPayment foundVehicleEntrantPayment = EntrantPayments.anyNotPaid();
    List<EntrantPaymentWithLatestPaymentDetailsDto> listWithUnpaidCazEntrantPaymentDto = Arrays
        .asList(
            EntrantPaymentWithLatestPaymentDetailsDto
                .fromEntrantPaymentOnly(foundVehicleEntrantPayment)
        );
    given(entrantPaymentService.bulkProcess(any()))
        .willReturn(listWithUnpaidCazEntrantPaymentDto);
  }

  private VehicleEntrantDto.VehicleEntrantDtoBuilder baseVehicleEntrantDtoBuilder() {
    return VehicleEntrantDto.builder()
        .cazEntryTimestamp(todayDateTime())
        .vrn(ANY_VALID_VRN)
        .cleanZoneId(ANY_CLEAN_ZONE_ID);
  }
}
