package ledger.controller;

import ledger.dto.TranEventDto;
import ledger.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping
@Tag(name = "Event Ledger API")
public class LedgerController {

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    private final LedgerService ledgerService;

    @PostMapping("/events")
    (summary = "Submit a event for transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfull"),
            @ApiResponse(responseCode = "200", description = "Return"),
            @ApiResponse(responseCode = "400", description = "Error"),
    })
    public ResponseEntity<TransactionEventDto> receiveEvent(@Valid @RequestBody TranEventDto eventDto) {
        TranEventDto result = ledgerService.processTransaction(eventDto);
        if (result.eventTimestamp().equals(eventDto.eventTimestamp())) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
      @GetMapping("/events/{id}")
    (summary = "Retrieve a single event by its ID")
    public ResponseEntity<TranEventDto> getEventById(@PathVariable("id") String id) {
        return ResponseEntity.ok(ledgerService.getEventById(id));
    }

     @GetMapping("/events")
    (summary = "List events for an account, sorted chronologically")
    public ResponseEntity<List<TranEventDto>> getEventsByAccount(@RequestParam("account") String accountId) {
        return ResponseEntity.ok(ledgerService.getEventsByAccount(accountId));
    }

  

  
}
