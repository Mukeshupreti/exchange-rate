package com.mukesh.fxservice.scheduler;

import com.mukesh.fxservice.service.ExchangeRateLoaderService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExchangeRateSchedulerTest {

    @Test
    void refreshRates_invokesLoader() {
        ExchangeRateLoaderService loader = mock(ExchangeRateLoaderService.class);
        ExchangeRateScheduler scheduler = new ExchangeRateScheduler(loader);

        scheduler.refreshRates();

        verify(loader).loadAllSupportedCurrencies();
    }
}

