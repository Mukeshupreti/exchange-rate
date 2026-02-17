package com.mukesh.fxservice.service;

import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.dto.ConversionResponse;
import com.mukesh.fxservice.exception.RateNotFoundException;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import com.mukesh.fxservice.config.CurrencyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExchangeRateServiceUnitTest {

    private ExchangeRateRepository repository;
    private ExchangeRateService service;
    private CurrencyProperties currencyProperties;

    @BeforeEach
    void setUp() {
        repository = mock(ExchangeRateRepository.class);
        currencyProperties = mock(CurrencyProperties.class);
        service = new ExchangeRateService(repository, currencyProperties);
    }

    @Test
    void convert_happyPath() {
        ExchangeRate rate = new ExchangeRate("USD", BigDecimal.valueOf(2), LocalDate.parse("2024-01-01"));
        when(repository.findByCurrencyAndRateDate(eq("USD"), any())).thenReturn(Optional.of(rate));

        ConversionResponse resp = service.convert("USD", BigDecimal.valueOf(10), LocalDate.parse("2024-01-01"));

        assertThat(resp.convertedAmount()).isEqualByComparingTo(new BigDecimal("5.000000"));
    }

    @Test
    void convert_zeroRate_throws() {
        ExchangeRate rate = new ExchangeRate("USD", BigDecimal.ZERO, LocalDate.parse("2024-01-01"));
        when(repository.findByCurrencyAndRateDate(eq("USD"), any())).thenReturn(Optional.of(rate));

        assertThrows(IllegalStateException.class, () -> service.convert("USD", BigDecimal.valueOf(10), LocalDate.parse("2024-01-01")));
    }

    @Test
    void getAllRates_whenEmpty_throwsNotFound() {
        when(repository.findAll(PageRequest.of(0, 10))).thenReturn(Page.empty());

        assertThrows(RateNotFoundException.class, () -> service.getAllRates(PageRequest.of(0, 10)));
    }

    @Test
    void getRatesByDate_whenEmpty_throwsNotFound() {
        LocalDate date = LocalDate.parse("2024-01-01");
        when(repository.findByRateDate(eq(date), eq(PageRequest.of(0, 10)))).thenReturn(Page.empty());

        assertThrows(RateNotFoundException.class, () -> service.getRatesByDate(date, PageRequest.of(0, 10)));
    }
}
