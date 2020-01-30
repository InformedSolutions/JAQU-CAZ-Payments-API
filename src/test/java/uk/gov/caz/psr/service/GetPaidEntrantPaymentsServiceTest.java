package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;
import uk.gov.caz.psr.util.TestObjectFactory.EntrantPayments;

@ExtendWith(MockitoExtension.class)
class GetPaidEntrantPaymentsServiceTest {

  @Mock
  private EntrantPaymentRepository entrantPaymentRepository;

  @InjectMocks
  private GetPaidEntrantPaymentsService getPaidEntrantPaymentsService;

  private final static String ANY_VRN_1 = "CAS123";
  private final static String ANY_VRN_2 = "CAS125";
  private final static List<String> ANY_VRNS_LIST = Arrays.asList(ANY_VRN_1, ANY_VRN_2);
  private final static LocalDate ANY_START_DATE = LocalDate.of(2020, 1, 1);
  private final static LocalDate ANY_END_DATE = LocalDate.of(2020, 2, 1);
  private final static UUID ANY_UUID = UUID.fromString("6cb5a6b1-18ae-4b06-ac29-f8433099381c");

  @Test
  public void shouldReturnEmptyCollectionWhenNoDatesArePaid() {
    // given
    mockEmptyResultFromEntrantPaymentRepository();

    // when
    Map<String, List<EntrantPayment>> result = getPaidEntrantPaymentsService
        .getResults(ANY_VRNS_LIST, ANY_START_DATE, ANY_END_DATE, ANY_UUID);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  public void shouldReturnCollectionOfPaidDatesWhenThereArePaidDates() {
    // given
    mockNonEmptyResultFromEntrantPaymentRepository();

    // when
    Map<String, List<EntrantPayment>> result = getPaidEntrantPaymentsService
        .getResults(ANY_VRNS_LIST, ANY_START_DATE, ANY_END_DATE, ANY_UUID);

    // then
    assertThat(result).isNotEmpty();
    assertThat(result.size()).isEqualTo(2);
  }

  private void mockEmptyResultFromEntrantPaymentRepository() {
    when(entrantPaymentRepository.findAllPaidByVrnAndDateRangeAndCazId(any(), any(), any(), any()))
        .thenReturn(new ArrayList<>());
  }

  private void mockNonEmptyResultFromEntrantPaymentRepository() {
    when(entrantPaymentRepository.findAllPaidByVrnAndDateRangeAndCazId(any(), any(), any(), any()))
        .thenReturn(buildMockedEntrantPaymentRepositoryResult());
  }

  private List<EntrantPayment> buildMockedEntrantPaymentRepositoryResult() {
    return Arrays.asList(
        buildEntrantPaymentForVrn(ANY_VRN_1),
        buildEntrantPaymentForVrn(ANY_VRN_2)
    );
  }

  private EntrantPayment buildEntrantPaymentForVrn(String vrn) {
    return EntrantPayments.anyPaid().toBuilder()
        .vrn(vrn)
        .travelDate(LocalDate.of(2020, 1, 15))
        .cleanAirZoneId(ANY_UUID)
        .build();
  }
}