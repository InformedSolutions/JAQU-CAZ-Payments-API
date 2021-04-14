package uk.gov.caz.psr.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ModificationHistoryDetailsTest {

  private static final String VRN = "CAS123";

  @Test
  void toStringMasksTheVrnValue() {
    ModificationHistoryDetails modificationHistoryDetails = ModificationHistoryDetails.builder()
        .vrn(VRN)
        .build();

    assertThat(modificationHistoryDetails.toString().contains(VRN)).isFalse();
  }
}