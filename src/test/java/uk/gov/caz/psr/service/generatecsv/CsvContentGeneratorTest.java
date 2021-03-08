package uk.gov.caz.psr.service.generatecsv;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.lenient;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.repository.audit.PaymentDetailRepository;
import uk.gov.caz.psr.repository.generatecsv.CsvEntrantPaymentRepository;
import uk.gov.caz.psr.util.CurrencyFormatter;

@ExtendWith(MockitoExtension.class)
class CsvContentGeneratorTest {

  private final static UUID CAZ_1_ID = UUID.randomUUID();
  private final static UUID CAZ_2_ID = UUID.randomUUID();

  private CsvContentGenerator csvGeneratorService;

  @Mock
  private AccountsRepository accountsRepository;

  @Mock
  private CsvEntrantPaymentRepository csvEntrantPaymentRepository;

  @Mock
  CurrencyFormatter currencyFormatter;

  @Mock
  private PaymentDetailRepository paymentDetailRepository;

  @Mock
  private VccsRepository vccsRepository;

  @BeforeEach
  public void setup() {
    csvGeneratorService = new CsvContentGenerator(accountsRepository, csvEntrantPaymentRepository,
        currencyFormatter, paymentDetailRepository, vccsRepository);
    mockClearAirZones();
  }

  private void mockClearAirZones() {
    CleanAirZoneDto caz1 = CleanAirZoneDto.builder()
        .cleanAirZoneId(CAZ_1_ID)
        .activeChargeStartDate("2021-08-20")
        .name("Birmingham")
        .build();
    CleanAirZoneDto caz2 = CleanAirZoneDto.builder()
        .cleanAirZoneId(CAZ_2_ID)
        .activeChargeStartDate("2023-08-20")
        .name("Test")
        .build();
    CleanAirZonesDto cleanAirZonesDto = CleanAirZonesDto.builder()
        .cleanAirZones(newArrayList(caz1, caz2))
        .build();
    Response<CleanAirZonesDto> cazResponse = Response.success(cleanAirZonesDto);
    lenient().when(vccsRepository.findCleanAirZonesSync()).thenReturn(cazResponse);
  }
}