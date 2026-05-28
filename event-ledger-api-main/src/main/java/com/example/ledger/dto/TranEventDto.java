package com.example.ledger.dto;

import com.example.ledger.entity.LedgerEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record TranEventDto(
        @NotBlank(message = "eventId required") String eventId,


        @NotBlank(message = "accountId required") String accountId,

        @NotNull(message = "type required") TransactionType type,

        @NotNull(message = "amount required") @Positive(message = "amount must be a positive number") BigDecimal amount,

        @NotBlank(message = "currency required") String currency,

        @NotNull(message = "eventTimestamp required") Instant eventTimestamp,

        Map<String, Object> metadata
) {
    public static TranEventDto fromEntity(LedgerEntry entry) {
        return new TranEventDto(
 		entry.getAmount(), entry.getCurrency(), entry.getEventTimestamp(), entry.getMetadata()
                entry.getEventId(), entry.getAccountId(), entry.getType(),
               
        );
    }
}
