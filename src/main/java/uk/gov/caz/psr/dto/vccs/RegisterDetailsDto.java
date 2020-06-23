package uk.gov.caz.psr.dto.vccs;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Class for single register details response object.
 */
@Value
@Builder
public class RegisterDetailsDto {

  /**
   * registerCompliant should be set to true if vehicle features on Retrofit, or is “compliant” in
   * GPW.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.registerDetails.registerCompliant}")
  boolean registerCompliant;

  /**
   * registerExempt should be set to true if vehicle features on MOD, or is “exempt” in GPW.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.registerDetails.registerExempt}")
  boolean registerExempt;

  /**
   * registeredMOD to be set to true if vehicle features in MOD.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.registerDetails.registeredMOD}")
  @JsonProperty("registeredMOD")
  boolean registeredMod;

  /**
   * registeredGPW to be set to true if vehicle features in GPW.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.registerDetails.registeredGPW}")
  @JsonProperty("registeredGPW")
  boolean registeredGpw;

  /**
   * registeredNTR to be set to true if vehicle features in NTR.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.registerDetails.registeredNTR}")
  @JsonProperty("registeredNTR")
  boolean registeredNtr;

  /**
   * registeredRetrofit to be set to true if vehicle features in Retrofit.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.registerDetails.registeredRetrofit}")
  boolean registeredRetrofit;
}