package com.mukesh.fxservice.integration;

import com.mukesh.fxservice.external.impl.BundesbankClient;
import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExchangeRateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExchangeRateRepository repository;

    @MockBean
    private BundesbankClient bundesbankClient;

    @BeforeEach
    void setup() {
        String mockCsv = """
                2024-01-09,1.100000,
                2024-01-10,1.200000,
                """;

        when(bundesbankClient.fetchExchangeRatesCsv("USD"))
                .thenReturn(mockCsv);

        repository.deleteAll();
        repository.save(new ExchangeRate("USD", new BigDecimal("1.200000"), LocalDate.parse("2024-01-10")));
    }


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


    @Test
    void shouldReturnRatesByDate() throws Exception {

        mockMvc.perform(get("/api/rates")
                        .param("date", "2024-01-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].currency")
                        .value(org.hamcrest.Matchers.hasItem("USD")));
    }


    @Test
    void shouldReturnBadRequestForInvalidCurrency() throws Exception {

        mockMvc.perform(get("/api/conversions")
                        .param("currency", "US")
                        .param("amount", "100")
                        .param("date", "2024-01-10"))
                .andExpect(status().isBadRequest());
    }
}
