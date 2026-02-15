package com.mukesh.fxservice.service;

import com.mukesh.fxservice.config.CurrencyProperties;
import com.mukesh.fxservice.external.BundesbankClient;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ExchangeRateServiceTest {

    private ExchangeRateRepository repository;
    private BundesbankClient client;
    private ExchangeRateService service;
    private ExchangeRateLoaderService loader;
    CurrencyProperties currencyProperties;
    @BeforeEach
    void setup() {
        repository = mock(ExchangeRateRepository.class);
        client = mock(BundesbankClient.class);
        currencyProperties=mock(CurrencyProperties.class);
        service = new ExchangeRateService(repository, loader,currencyProperties);
    }

    @Test
    void shouldFallbackWhenExternalFails() {

        when(client.fetchExchangeRatesCsv("USD"))
                .thenThrow(new RuntimeException("API down"));

        try {
            service.fetchAndStoreRates("USD");
        } catch (Exception ignored) {
        }

        verify(client, times(1))
                .fetchExchangeRatesCsv("USD");
    }
}
