package com.mukesh.fxservice.service;

import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.dto.ConversionResponse;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExchangeRateServiceFallbackTest {

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
    void getRate_fallbacksToLatestWhenExternalFails() {
        LocalDate queryDate = LocalDate.parse("2024-01-10");

        // no exact match
        when(repository.findByCurrencyAndRateDate(eq("USD"), eq(queryDate)))
                .thenReturn(Optional.empty());

        // loader fails when called
        when(loader.fetchAndStoreRates("USD")).thenThrow(new RuntimeException("down"));

        // fallback latest available
        ExchangeRate fallback = new ExchangeRate("USD", BigDecimal.valueOf(1.5), LocalDate.parse("2024-01-05"));
        when(repository.findTopByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(eq("USD"), eq(queryDate)))
                .thenReturn(Optional.of(fallback));

        ExchangeRate result = service.getRate("USD", queryDate);

        assertThat(result).isNotNull();
        assertThat(result.getRate()).isEqualByComparingTo(BigDecimal.valueOf(1.5));
    }

    @Test
    void convert_marksFallbackUsedWhenRateIsFromDifferentDate() {
        LocalDate queryDate = LocalDate.parse("2024-01-10");

        // no exact match
        when(repository.findByCurrencyAndRateDate(eq("USD"), eq(queryDate)))
                .thenReturn(Optional.empty());

        // loader fails
        when(loader.fetchAndStoreRates("USD")).thenThrow(new RuntimeException("down"));

        // fallback latest available
        ExchangeRate fallback = new ExchangeRate("USD", BigDecimal.valueOf(2), LocalDate.parse("2024-01-09"));
        when(repository.findTopByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(eq("USD"), eq(queryDate)))
                .thenReturn(Optional.of(fallback));

        ConversionResponse resp = service.convert("USD", BigDecimal.valueOf(10), queryDate);

        assertThat(resp.convertedAmount()).isEqualByComparingTo(new BigDecimal("5.000000"));
        assertThat(resp.fallbackUsed()).isTrue();
    }
}

