package uk.gov.caz.psr.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents the JSON structure for vehicle retrieval response for vehicles and charges
 * and stores cached charge.
 */
@Value
@Builder
public class CachedCharge {

  /**
   * Clean Air Zone Id.
   */
  UUID cazId;

  /**
   * Cached charge value.
   */
  Integer charge;

  /**
   * Cached tariffCode.m
   */
  String tariffCode;
}
