package com.zorvyn.finance.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinancialRecordCrudIntegrationTest {

    private static final String BASE64_SECRET =
        "zaKEFxDMYfquYbjluaBin3vUbzLCNMF0h21Ri3++L1M=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FinancialRecordRepository financialRecordRepository;

    private String adminJwt;

    @BeforeEach
    void setUp() {
        financialRecordRepository.deleteAll();
        adminJwt = generateToken("admin@zorvyn.local", "ADMIN");
    }

    @Test
    void createThenFetchRecord_shouldPersistAndReturnFromDatabase()
        throws Exception {
        String createPayload = """
            {
              "amount": 1250.75,
              "type": "INCOME",
              "category": "Freelance",
              "description": "Client payment",
              "transactionDate": "2026-03-25"
            }
            """;

        MvcResult createResult = mockMvc
            .perform(
                post("/api/records")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + adminJwt)
                    .content(createPayload)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.category").value("Freelance"))
            .andReturn();

        JsonNode created = objectMapper.readTree(
            createResult.getResponse().getContentAsString()
        );
        long recordId = created.get("id").asLong();

        mockMvc
            .perform(
                get("/api/records/{id}", recordId)
                    .header("Authorization", "Bearer " + adminJwt)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(recordId))
            .andExpect(jsonPath("$.amount").value(1250.75))
            .andExpect(jsonPath("$.type").value("INCOME"));

        FinancialRecord persisted = financialRecordRepository
            .findByIdAndDeletedFalse(recordId)
            .orElseThrow();
        assertThat(persisted.getCategory()).isEqualTo("Freelance");
        assertThat(persisted.getDescription()).isEqualTo("Client payment");
    }

    private String generateToken(String subject, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(120, ChronoUnit.MINUTES);

        return Jwts.builder()
            .subject(subject)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_SECRET)))
            .compact();
    }
}
