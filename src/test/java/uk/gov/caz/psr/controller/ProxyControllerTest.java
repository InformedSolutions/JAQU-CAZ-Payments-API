package uk.gov.caz.psr.controller;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.RestAssured.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.service.CleanAirZoneService;
import uk.gov.caz.psr.service.VehicleComplianceRetrievalService;
import uk.gov.caz.psr.service.WhitelistService;

@ContextConfiguration(classes = {ExceptionController.class, Configuration.class,
    ProxyController.class})
@WebMvcTest
class ProxyControllerTest {

  @MockBean
  private CleanAirZoneService cleanAirZoneService;

  @MockBean
  private VehicleComplianceRetrievalService vehicleComplianceRetrievalService;

  @MockBean
  private WhitelistService whitelistService;

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  public void resetMocks() {
    Mockito.reset(cleanAirZoneService);
    Mockito.reset(vehicleComplianceRetrievalService);
  }

  private static final String ANY_CLEAN_AIR_ZONE_ID = UUID.randomUUID().toString();
  private static final String ANY_CORRELATION_ID = UUID.randomUUID().toString();

  private static final String GET_CLEAN_AIR_ZONES_PATH =
      ProxyController.BASE_PATH + "/"
          + ProxyController.GET_CLEAN_AIR_ZONES;

  private static final String GET_COMPLIANCE_PATH =
      ProxyController.BASE_PATH + "/"
          + ProxyController.GET_COMPLIANCE;

  private static final String POST_BULK_COMPLIANCE_PATH =
      ProxyController.BASE_PATH + "/"
          + ProxyController.POST_BULK_COMPLIANCE;

  private static final String GET_VEHICLE_DETAILS_PATH =
      ProxyController.BASE_PATH + "/"
          + ProxyController.GET_VEHICLE_DETAILS;

  private static final String GET_UNRECOGNISED_VEHICLE_COMPLIANCE_PATH =
      ProxyController.BASE_PATH + "/"
          + ProxyController.GET_UNRECOGNISED_VEHICLE_COMPLIANCE;

  private static final String GET_REGISTER_DETAILS_PATH =
      ProxyController.BASE_PATH + "/"
          + ProxyController.GET_REGISTER_DETAILS;

  @Nested
  class GetCleanAirZones {

    @Test
    public void shouldReturn400StatusCodeWhenCleanAirZonesAreFetchedWithoutCorrelationId()
        throws Exception {
      mockMvc
          .perform(get(GET_CLEAN_AIR_ZONES_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().is4xxClientError()).andExpect(jsonPath("message")
          .value("Missing request header 'X-Correlation-ID'"));
    }
  }

  @Nested
  class GetCompliance {

    @Test
    public void shouldReturn400StatusCodeWhenComplianceFetchedWithoutCorrelationId()
        throws Exception {
      mockMvc
          .perform(get(GET_COMPLIANCE_PATH.replace("{vrn}", "TESTVRN"))
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .param("zones", ANY_CLEAN_AIR_ZONE_ID))
          .andExpect(status().is4xxClientError()).andExpect(jsonPath("message")
          .value("Missing request header 'X-Correlation-ID'"));
    }
  }

  @Nested
  class BulkCompliance {

    @Test
    public void shouldReturn200StatusCodeAndResultsForCorrectParameters()
        throws Exception {
      String payload = "[\"vrn1\"]";
      ComplianceResultsDto resultsDto = ComplianceResultsDto.builder()
          .isExempt(false)
          .isRetrofitted(false)
          .registrationNumber("vrn1")
          .vehicleType("taxi")
          .build();
      BDDMockito.given(
          vehicleComplianceRetrievalService.retrieveVehicleCompliance(newArrayList("vrn1"), null))
          .willReturn(newArrayList(resultsDto));
      mockMvc
          .perform(post(POST_BULK_COMPLIANCE_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .content(payload)
              .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().is2xxSuccessful())
          .andExpect(jsonPath("$[0].registrationNumber").value("vrn1"))
          .andExpect(jsonPath("$[0].vehicleType").value("taxi"))
          .andExpect(jsonPath("$[0].isExempt").value("false"))
          .andExpect(jsonPath("$[0].isRetrofitted").value("false"));
    }

    @Test
    public void shouldReturn400StatusCodeWhenBulkComplianceRequestedWithoutListOfVrns()
        throws Exception {
      String payload = "";
      mockMvc
          .perform(post(POST_BULK_COMPLIANCE_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .content(payload)
              .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldReturn400StatusCodeWhenBulkComplianceRequestedWithoutCorrelationId()
        throws Exception {
      mockMvc
          .perform(post(POST_BULK_COMPLIANCE_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .param("zones", ANY_CLEAN_AIR_ZONE_ID))
          .andExpect(status().is4xxClientError()).andExpect(jsonPath("message")
          .value("Missing request header 'X-Correlation-ID'"));
    }
  }

  @Nested
  class VehicleDetails {

    @Test
    public void shouldReturn400StatusCodeWhenVehicleDetailsFetchedWithoutCorrelationId()
        throws Exception {
      mockMvc
          .perform(get(GET_VEHICLE_DETAILS_PATH.replace("{vrn}", "TESTVRN"))
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .param("zones", ANY_CLEAN_AIR_ZONE_ID))
          .andExpect(status().is4xxClientError()).andExpect(jsonPath("message")
          .value("Missing request header 'X-Correlation-ID'"));
    }
  }

  @Nested
  class UnknownVehicleCompliance {

    @Test
    public void shouldReturn400StatusCodeWhenVehicleDetailsFetchedWithoutCorrelationId()
        throws Exception {
      mockMvc
          .perform(get(GET_UNRECOGNISED_VEHICLE_COMPLIANCE_PATH.replace("{type}", "CAR"))
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .param("zones", ANY_CLEAN_AIR_ZONE_ID))
          .andExpect(status().is4xxClientError()).andExpect(jsonPath("message")
          .value("Missing request header 'X-Correlation-ID'"));
    }
  }

  @Nested
  class RegisterDetails {

    @Test
    public void shouldReturn400StatusCodeWhenRegisterDetailsFetchedWithoutCorrelationId()
        throws Exception {
      mockMvc
          .perform(get(GET_REGISTER_DETAILS_PATH.replace("{vrn}", "CAS312"))
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().is4xxClientError()).andExpect(jsonPath("message")
          .value("Missing request header 'X-Correlation-ID'"));
    }
  }
}
