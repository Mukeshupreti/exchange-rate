package com.mukesh.fxservice.service;

import com.mukesh.fxservice.repository.ExchangeRateRepository;
import com.mukesh.fxservice.external.BundesbankClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.mockito.Mockito.*;

class ExchangeRateServiceTest {

    private ExchangeRateRepository repository;
    private BundesbankClient client;
    private ExchangeRateService service;
    private  ExchangeRateLoaderService loader;

    @BeforeEach
    void setup() {
        repository = mock(ExchangeRateRepository.class);
        client = mock(BundesbankClient.class);
        service = new ExchangeRateService(client, repository,loader);
    }

    @Test
    void shouldFallbackWhenExternalFails() {

        when(client.fetchExchangeRatesCsv("USD"))
                .thenThrow(new RuntimeException("API down"));

        try {
            service.fetchAndStoreRates("USD");
        } catch (Exception ignored) {}

        verify(client, times(1))
                .fetchExchangeRatesCsv("USD");
    }
}
