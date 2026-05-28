package ledger.entity;

import ledger.dto.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_account_timestamp", columnList = "accountId, eventTimestamp")
})
public class LedgerEntry {

    public LedgerEntry(String eventId, String accountId, TransactionType type, BigDecimal amount,
                       String currency, Instant eventTimestamp, Map<String, String> metadata) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.eventTimestamp = eventTimestamp;
        this.metadata = metadata;
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    
    @Column(nullable = false)
    private Instant eventTimestamp;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String accountId;
    
    @Column(nullable = false)
    private String currency;

    @Column(nullable = false) @Enumerated(EnumType.STRING)
    private TransactionType type;

   
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ledger_entry_metadata", joinColumns = @JoinColumn(name = "entry_id"))
    private Map<String, String> metadata;
   

    public LedgerEntry() {}

   

   }
