package uk.gov.caz.psr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.psr.dto.VehicleEntrantRequest;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.service.VehicleEntrantService;
import uk.gov.caz.psr.util.TestObjectFactory.VehicleEntrantPayments;

@ContextConfiguration(classes = {Configuration.class, VehicleEntrantController.class})
@WebMvcTest
class VehicleEntrantControllerTest {

  @MockBean
  private VehicleEntrantService vehicleEntrantService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";
  private static final UUID ANY_CLEAN_ZONE_ID =
      UUID.fromString("e41d36a6-fcd6-4467-9368-af75201dcae2");
  private static final String ANY_VALID_VRN = "DL76MWX";
  private static final String PATH = VehicleEntrantController.BASE_PATH + "/"
      + VehicleEntrantController.CREATE_VEHICLE_ENTRANT_PATH_AND_GET_PAYMENT_DETAILS;

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

      verify(vehicleEntrantService, never()).registerVehicleEntrant(any());
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

      verify(vehicleEntrantService, never()).registerVehicleEntrant(any());
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

      verify(vehicleEntrantService, never()).registerVehicleEntrant(any());
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

      verify(vehicleEntrantService, never()).registerVehicleEntrant(any());
    }
  }

  @Test
  public void shouldReturn200StatusCodeAndPaymentStatusNotPaidWhenNotPaid() throws Exception {
    String payload = requestWithValidBody();
    VehicleEntrantPayment foundVehicleEntrantPayment = VehicleEntrantPayments.anyNotPaid();
    given(vehicleEntrantService.registerVehicleEntrant(any()))
        .willReturn(foundVehicleEntrantPayment.getInternalPaymentStatus());

    mockMvc
        .perform(post(PATH).content(payload)
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(jsonPath("$.status").value(InternalPaymentStatus.NOT_PAID.name()));

    verify(vehicleEntrantService).registerVehicleEntrant(any());
  }

  @Test
  public void shouldReturn200StatusCodeAndPaymentStatusNotPaidWhenPaidPayment() throws Exception {
    String payload = requestWithValidBody();
    VehicleEntrantPayment foundVehicleEntrantPayment = VehicleEntrantPayments.anyPaid();
    given(vehicleEntrantService.registerVehicleEntrant(any()))
        .willReturn(foundVehicleEntrantPayment.getInternalPaymentStatus());

    mockMvc
        .perform(post(PATH).content(payload)
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(jsonPath("$.status").value(InternalPaymentStatus.PAID.name()));

    verify(vehicleEntrantService).registerVehicleEntrant(any());
  }

  @SneakyThrows
  private String toJson(VehicleEntrantRequest request) {
    return objectMapper.writeValueAsString(request);
  }

  private LocalDateTime todayDateTime() {
    return LocalDateTime.now();
  }

  private String requestWithCleanZoneId(UUID cleanZoneId) {
    VehicleEntrantRequest request = baseRequestBuilder().cleanZoneId(cleanZoneId).build();
    return toJson(request);
  }

  private String requestWithCazEntryTimestamp(LocalDateTime cazEntryTimestamp) {
    VehicleEntrantRequest request =
        baseRequestBuilder().cazEntryTimestamp(cazEntryTimestamp).build();
    return toJson(request);
  }

  private String requestWithVrn(String vrn) {
    VehicleEntrantRequest request = baseRequestBuilder().vrn(vrn).build();
    return toJson(request);
  }

  private String requestWithValidBody() {
    VehicleEntrantRequest request = baseRequestBuilder().build();
    return toJson(request);
  }

  private VehicleEntrantRequest.VehicleEntrantRequestBuilder baseRequestBuilder() {
    return VehicleEntrantRequest.builder().cleanZoneId(ANY_CLEAN_ZONE_ID).vrn(ANY_VALID_VRN)
        .cazEntryTimestamp(todayDateTime());
  }
}
