package com.mukesh.fxservice.service;

import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.dto.ConversionResponse;
import com.mukesh.fxservice.exception.RateNotFoundException;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ExchangeRateServiceUnitTest {

    private ExchangeRateRepository repository;
    private ExchangeRateLoaderService loader;
    private ExchangeRateService service;

    @BeforeEach
    void setUp() {
        repository = mock(ExchangeRateRepository.class);
        loader = mock(ExchangeRateLoaderService.class);
        service = new ExchangeRateService(repository, loader, null);
    }

    @Test
    void convert_happyPath() {
        ExchangeRate rate = new ExchangeRate("USD", BigDecimal.valueOf(2), LocalDate.parse("2024-01-01"));
        when(repository.findByCurrencyAndRateDate(eq("USD"), any())).thenReturn(Optional.of(rate));

        ConversionResponse resp = service.convert("USD", BigDecimal.valueOf(10), LocalDate.parse("2024-01-01"));

        // amount 10 / rate 2 = 5
        assertThat(resp.convertedAmount()).isEqualByComparingTo(new BigDecimal("5.000000"));
    }

    @Test
    void convert_zeroRate_throws() {
        ExchangeRate rate = new ExchangeRate("USD", BigDecimal.ZERO, LocalDate.parse("2024-01-01"));
        when(repository.findByCurrencyAndRateDate(eq("USD"), any())).thenReturn(Optional.of(rate));

        assertThrows(IllegalStateException.class, () -> service.convert("USD", BigDecimal.valueOf(10), LocalDate.parse("2024-01-01")));
    }
}
