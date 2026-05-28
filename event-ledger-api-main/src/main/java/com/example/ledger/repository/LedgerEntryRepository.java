package ledger.repository;

import ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    Optional<LedgerEntry> findByEventId(String eventId);
    List<LedgerEntry> findByAccountIdOrderByEventTimestampAsc(String accountId);
}
