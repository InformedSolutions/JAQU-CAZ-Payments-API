package uk.gov.caz.psr.service.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.caz.psr.repository.audit.PaymentDetailRepository;
import uk.gov.caz.psr.repository.audit.PaymentLoggedActionRepository;

@ExtendWith(MockitoExtension.class)
public class PaymentDataCleanupServiceTest {

  @Mock
  private PaymentLoggedActionRepository paymentLoggedActionRepository;
  @Mock
  private PaymentDetailRepository paymentDetailRepository;

  private PaymentDataCleanupService service;

  @Test
  public void shouldThrowExceptionWhenRepositoryFailToExecuteSQLStatement() {
    // given
    when(paymentLoggedActionRepository.deleteLogsBeforeDate(any(LocalDate.class))).thenThrow(RuntimeException.class);
    service = new PaymentDataCleanupService(paymentLoggedActionRepository,paymentDetailRepository,18,18);
    // then
    Assertions.assertThrows(RuntimeException.class, () -> {
      service.cleanupData();
    });
  }
}