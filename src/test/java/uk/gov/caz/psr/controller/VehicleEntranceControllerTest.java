package uk.gov.caz.psr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
import uk.gov.caz.psr.dto.VehicleEntranceRequest;
import uk.gov.caz.psr.service.VehicleEntranceService;

@ContextConfiguration(classes = {Configuration.class,
    VehicleEntranceController.class})
@WebMvcTest
class VehicleEntranceControllerTest {

  @MockBean
  private VehicleEntranceService vehicleEntranceService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";
  private static final UUID ANY_CLEAN_ZONE_ID = UUID.fromString(
      "e41d36a6-fcd6-4467-9368-af75201dcae2");
  private static final String ANY_VALID_VRN = "DL76MWX";

  private static final String PATH = VehicleEntranceController.BASE_PATH
      + "/" + VehicleEntranceController.CREATE_VEHICLE_ENTRANCE_PATH_AND_GET_PAYMENT_DETAILS;

  @Nested
  class Validation {
    @Test
    public void shouldReturn400StatusCodeWhenCleanZoneIdIsNull() throws Exception {
      String payload = toJson(new VehicleEntranceRequest(null, today(), ANY_VALID_VRN));

      mockMvc.perform(post(PATH)
          .content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
      // TODO add assertions for a message

      verify(vehicleEntranceService, never()).registerVehicleEntrance(any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenDateOfEntranceIsNull() throws Exception {
      String payload = toJson(new VehicleEntranceRequest(ANY_CLEAN_ZONE_ID, null, ANY_VALID_VRN));

      mockMvc.perform(post(PATH)
          .content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
      // TODO add assertions for a message

      verify(vehicleEntranceService, never()).registerVehicleEntrance(any());
    }

    @Test
    public void shouldReturn400StatusCodeWhenVrnIsNull() throws Exception {
      String payload = toJson(new VehicleEntranceRequest(ANY_CLEAN_ZONE_ID, today(), null));

      mockMvc.perform(post(PATH)
          .content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
      // TODO add assertions for a message

      verify(vehicleEntranceService, never()).registerVehicleEntrance(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "aaaaaaaaaaaaaaaa"})
    public void shouldReturn400StatusCodeWhenVrnIsInvalid(String vrn) throws Exception {
      String payload = toJson(new VehicleEntranceRequest(ANY_CLEAN_ZONE_ID, today(), vrn));

      mockMvc.perform(post(PATH)
          .content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
      // TODO add assertions for a message

      verify(vehicleEntranceService, never()).registerVehicleEntrance(any());
    }
  }

  @Test
  public void shouldReturn200StatusCodeWhenRequestIsValid() throws Exception {
    String payload = toJson(new VehicleEntranceRequest(ANY_CLEAN_ZONE_ID, today(), ANY_VALID_VRN));

    mockMvc.perform(post(PATH)
        .content(payload)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isOk())
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));

    verify(vehicleEntranceService).registerVehicleEntrance(any());
  }

  @SneakyThrows
  private String toJson(VehicleEntranceRequest request) {
    return objectMapper.writeValueAsString(request);
  }

  private LocalDateTime today() {
    return LocalDateTime.now();
  }
}