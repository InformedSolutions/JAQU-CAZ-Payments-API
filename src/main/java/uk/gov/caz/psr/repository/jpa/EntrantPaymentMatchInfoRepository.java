package uk.gov.caz.psr.repository.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;

/**
 * Jpa Specification database repository used for fetching payment-info-related data.
 */
public interface EntrantPaymentMatchInfoRepository extends
    PagingAndSortingRepository<EntrantPaymentMatchInfo, UUID>,
    JpaSpecificationExecutor<EntrantPaymentMatchInfo> {
}
