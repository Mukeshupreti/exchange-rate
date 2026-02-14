package com.mukesh.fxservice.controller;

import com.mukesh.fxservice.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExchangeRateController.class)
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService service;

    @Test
    void shouldConvertCurrencySuccessfully() throws Exception {

        when(service.convert("USD",
                new BigDecimal("100"),
                LocalDate.parse("2024-01-10")))
                .thenReturn(null); // you can mock proper DTO here

        mockMvc.perform(get("/api/conversions")
                        .param("currency", "USD")
                        .param("amount", "100")
                        .param("date", "2024-01-10"))
                .andExpect(status().isOk());
    }
    @Test
    void shouldReturnBadRequestForInvalidAmount() throws Exception {

        mockMvc.perform(get("/api/conversions")
                        .param("currency", "USD")
                        .param("amount", "-10")
                        .param("date", "2024-01-10"))
                .andExpect(status().isBadRequest());
    }

}
