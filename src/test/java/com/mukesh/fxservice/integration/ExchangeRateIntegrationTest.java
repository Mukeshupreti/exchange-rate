package com.mukesh.fxservice.integration;

import com.mukesh.fxservice.external.BundesbankClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExchangeRateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BundesbankClient bundesbankClient;

    private String mockCsv;

    @BeforeEach
    void setup() {
        mockCsv = """
                2024-01-09,1.100000,
                2024-01-10,1.200000,
                """;

        when(bundesbankClient.fetchExchangeRatesCsv("USD"))
                .thenReturn(mockCsv);
    }

    // =========================================
    // 1️⃣ Test Lazy Load + Convert
    // =========================================
    @Test
    void shouldLoadRatesAndConvertSuccessfully() throws Exception {

        mockMvc.perform(get("/api/conversions")
                        .param("currency", "USD")
                        .param("amount", "120")
                        .param("date", "2024-01-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.convertedAmount").exists())
                .andExpect(jsonPath("$.rateDate").value("2024-01-10"));
    }

    // =========================================
    // 2️⃣ Test Get Rates By Date
    // =========================================
    @Test
    void shouldReturnRatesByDate() throws Exception {

        mockMvc.perform(get("/api/rates")
                        .param("date", "2024-01-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currency").value("USD"))
                .andExpect(jsonPath("$.content[0].rate").value(1.200000));
    }

    // =========================================
    // 3️⃣ Test Validation
    // =========================================
    @Test
    void shouldReturnBadRequestForInvalidCurrency() throws Exception {

        mockMvc.perform(get("/api/conversions")
                        .param("currency", "US")
                        .param("amount", "100")
                        .param("date", "2024-01-10"))
                .andExpect(status().isBadRequest());
    }
}
