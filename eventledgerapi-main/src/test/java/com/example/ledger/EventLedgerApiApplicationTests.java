package com.example.ledger;

import com.example.ledger.dto.TransactionEventDto;
import com.example.ledger.dto.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class EventLedgerApiApplicationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private TransactionEventDto creditEvent;
    private TransactionEventDto debitEvent;



    @BeforeEach
    public void setup() {
        creditEvent = new TransactionEventDto("evt-001", "acct-123", TransactionType.CREDIT,
                new BigDecimal("150.00"), "USD", Instant.parse("2026-05-15T14:00:00Z"), Collections.emptyMap());
        debitEvent = new TransactionEventDto("evt-002", "acct-123", TransactionType.DEBIT,
                new BigDecimal("50.00"), "USD", Instant.parse("2026-05-15T15:00:00Z"), Collections.emptyMap());
    }

    @Test
    public void testSubmissionAndBalance() throws Exception {
        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(creditEvent)))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/accounts/acct-123/balance")).andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    public void testIdempotency() throws Exception {
        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(creditEvent)));
        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(creditEvent)))
                .andExpect(status().isOk());
    }

/*
    @Test
    public void testOutOfOrderArrivalAndSorting() throws Exception {
        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(debitEvent)));
        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(creditEvent)));
        mockMvc.perform(get("/accounts/acct-123/balance")).andExpect(jsonPath("$.balance").value(100.00));
        mockMvc.perform(get("/events?account=acct-123")).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$.eventId").value("evt-001"));
    }
*/

    @Test
    public void testHighConcurrencySimultaneousSubmissions() throws Exception {
        int numberOfThreads = 8;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        String concurrentPayload = objectMapper.writeValueAsString(creditEvent);

        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                try {
                    latch.await();
                    mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(concurrentPayload))
                            .andDo(result -> {
                                if (result.getResponse().getStatus() == 200) duplicateCount.incrementAndGet();
                            });
                } catch (Exception ignored) {}
            });
        }

        latch.countDown();
        service.shutdown();
        while (!service.isTerminated()) { Thread.sleep(10); }

        org.junit.jupiter.api.Assertions.assertEquals(numberOfThreads - 7, duplicateCount.get());
    }

    @Test
    public void submissionWithInvalidTransactionTypeShouldReturnBadRequest() throws Exception {
        TransactionEventDto invalidEvent = new TransactionEventDto("evt-003", "acct-123", null,
                new BigDecimal("100.00"), "USD", Instant.parse("2026-05-15T16:00:00Z"), Collections.emptyMap());

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void submissionWithNegativeAmountShouldReturnBadRequest() throws Exception {
        TransactionEventDto invalidEvent = new TransactionEventDto("evt-004", "acct-123", TransactionType.CREDIT,
                new BigDecimal("-100.00"), "USD", Instant.parse("2026-05-15T16:00:00Z"), Collections.emptyMap());

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void submissionWithMissingAccountIdShouldReturnBadRequest() throws Exception {
        TransactionEventDto invalidEvent = new TransactionEventDto("evt-005", null, TransactionType.CREDIT,
                new BigDecimal("100.00"), "USD", Instant.parse("2026-05-15T16:00:00Z"), Collections.emptyMap());

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void submissionWithFutureTimestampShouldBeAccepted() throws Exception {
        TransactionEventDto futureEvent = new TransactionEventDto("evt-006", "acct-123", TransactionType.CREDIT,
                new BigDecimal("200.00"), "USD", Instant.now().plusSeconds(3600), Collections.emptyMap());

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(futureEvent)))
                .andExpect(status().is2xxSuccessful());
    }


    @Test
    public void getEventsForNonExistentAccountShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/events?account=non-existent-account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testOutOfOrderToleranceAndChronologicalSorting_Positive() throws Exception {
        // Submit later event first (15:00:00 DEBIT)
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitEvent)));

        // Submit earlier event second (14:00:00 CREDIT)
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditEvent)));

        // Net balance calculated correctly regardless of arrival sequence (\$150 - \$50 = \$100)
        mockMvc.perform(get("/accounts/acct-123/balance"))
                .andExpect(jsonPath("$.balance").value(100.00));

        // Event listing query MUST return sorted timeline chronologically: evt-001 (14:00) then evt-002 (15:00)
        mockMvc.perform(get("/events?account=acct-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventId").value("evt-001"))
                .andExpect(jsonPath("$[1].eventId").value("evt-002"));
    }

    @Test
    public void testSubmitEventWithNegativeAndZeroAmount_Negative() throws Exception {
        // Case A: Zero Amount
        TransactionEventDto zeroAmountEvent = new TransactionEventDto(
                "evt-003", "acct-123", TransactionType.CREDIT,
                BigDecimal.ZERO, "USD", Instant.now(), Collections.emptyMap()
        );
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroAmountEvent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").exists());

        // Case B: Negative Amount
        TransactionEventDto negativeAmountEvent = new TransactionEventDto(
                "evt-004", "acct-123", TransactionType.CREDIT,
                new BigDecimal("-25.50"), "USD", Instant.now(), Collections.emptyMap()
        );
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeAmountEvent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").exists());
    }

    @Test
    public void testSubmitEventWithMissingRequiredFields_Negative() throws Exception {
        // Missing accountId and eventTimestamp entirely
        String malformedJsonPayload = """
                {
                  "eventId": "evt-005",
                  "type": "CREDIT",
                  "amount": 100.00,
                  "currency": "USD"
                }
                """;

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accountId").exists())
                .andExpect(jsonPath("$.eventTimestamp").exists());
    }

    @Test
    public void testSubmitEventWithUnknownType_Negative() throws Exception {
        // Providing anything other than CREDIT or DEBIT should be caught by Jackson mapping validation
        String badTypeJsonPayload = """
                {
                  "eventId": "evt-006",
                  "accountId": "acct-123",
                  "type": "TRANSFER_OUT",
                  "amount": 100.00,
                  "currency": "USD",
                  "eventTimestamp": "2026-05-15T14:02:11Z"
                  }
                  """;

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badTypeJsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("Must be either CREDIT or DEBIT"));
    }

    @Test
    public void testGetEventById_Positive() throws Exception {
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditEvent)));

        mockMvc.perform(get("/events/evt-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-001"))
                .andExpect(jsonPath("$.accountId").value("acct-123"));
    }

}
