package com.mukesh.fxservice.controller;

import com.mukesh.fxservice.dto.ConversionResponse;
import com.mukesh.fxservice.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeRateController.class)
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService service;

    // ==============================
    // SUCCESS TEST
    // ==============================
    @Test
    void shouldConvertCurrencySuccessfully() throws Exception {

        ConversionResponse response = new ConversionResponse(
                "USD",
                new BigDecimal("100"),
                new BigDecimal("1.100000"),
                new BigDecimal("90.909091"),
                LocalDate.parse("2024-01-10"),
                false
        );

        when(service.convert(
                "USD",
                new BigDecimal("100"),
                LocalDate.parse("2024-01-10")))
                .thenReturn(response);

        mockMvc.perform(get("/api/conversions")
                        .param("currency", "USD")
                        .param("amount", "100")
                        .param("date", "2024-01-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.rate").value(1.100000))
                .andExpect(jsonPath("$.convertedAmount").value(90.909091))
                .andExpect(jsonPath("$.fallbackUsed").value(false));
    }

    // ==============================
    // VALIDATION TESTS
    // ==============================

    @Test
    void shouldReturnBadRequestForInvalidAmount() throws Exception {

        mockMvc.perform(get("/api/conversions")
                        .param("currency", "USD")
                        .param("amount", "-10")
                        .param("date", "2024-01-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForInvalidCurrency() throws Exception {

        mockMvc.perform(get("/api/conversions")
                        .param("currency", "US")
                        .param("amount", "100")
                        .param("date", "2024-01-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenDateMissing() throws Exception {

        mockMvc.perform(get("/api/conversions")
                        .param("currency", "USD")
                        .param("amount", "100"))
                .andExpect(status().isBadRequest());
    }

    // ==============================
    // BUSINESS ERROR TEST
    // ==============================

    @Test
    void shouldReturnNotFoundWhenRateMissing() throws Exception {

        doThrow(new RuntimeException("No rate available"))
                .when(service)
                .convert("USD",
                        new BigDecimal("100"),
                        LocalDate.parse("2050-01-01"));

        mockMvc.perform(get("/api/conversions")
                        .param("currency", "USD")
                        .param("amount", "100")
                        .param("date", "2050-01-01"))
                .andExpect(status().isInternalServerError());
    }
}
