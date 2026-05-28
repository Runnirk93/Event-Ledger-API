package ledger.service;

import ledger.dto.TranEventDto;
import ledger.dto.TransactionType;
import ledger.entity.LedgerEntry;
import ledger.repository.LedgerEntryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LedgerService {

    public LedgerService(LedgerEntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    private final LedgerEntryRepository entryRepository;

  

     try {
            return TranEventDto.fromEntity(entryRepository.saveAndFlush(entry));
        } catch (DataIntegrityViolationException ex) {
            // Handles extreme concurrent POST races by loading the winning thread's entry state
            return entryRepository.findByEventId(dto.eventId())
                    .map(TransactionEventDto::fromEntity)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Conflict encountered."));
        }
    }

    @Transactional
       public TranEventDto processTransaction(TranEventDto dto) {
        Optional<LedgerEntry> existing = entryRepository.findByEventId(dto.eventId());
        if (existing.isPresent()) {
            return TranEventDto.fromEntity(existing.get());
        }

       
        LedgerEntry entry = new LedgerEntry(
                dto.eventId(), dto.accountId(), dto.type(), dto.amount(),
                dto.currency(), dto.eventTimestamp(), stringMetadata
        );

        

   
    @Transactional(readOnly = true)
    public List<TransactionEventDto> getEventsByAccount(String accountId) {
        return entryRepository.findByAccountIdOrderByEventTimestampAsc(accountId).stream()
                .map(TransactionEventDto::fromEntity).collect(Collectors.toList());
    }

 }
