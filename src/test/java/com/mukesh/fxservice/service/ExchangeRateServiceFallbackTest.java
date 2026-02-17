package com.mukesh.fxservice.service;

import com.mukesh.fxservice.exception.RateNotFoundException;
import com.mukesh.fxservice.config.CurrencyProperties;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExchangeRateServiceFallbackTest {

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
    void getRate_missingExactDate_returnsNotFound() {
        LocalDate queryDate = LocalDate.parse("2024-01-10");

        when(repository.findByCurrencyAndRateDate(eq("USD"), eq(queryDate)))
                .thenReturn(Optional.empty());

        assertThrows(RateNotFoundException.class, () -> service.getRate("USD", queryDate));
    }
}
