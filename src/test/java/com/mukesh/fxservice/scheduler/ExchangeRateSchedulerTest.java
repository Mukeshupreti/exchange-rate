package com.mukesh.fxservice.scheduler;

import com.mukesh.fxservice.service.api.ExchangeRateLoader;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExchangeRateSchedulerTest {

    @Test
    void refreshRates_invokesLoader() {
        ExchangeRateLoader loader = mock(ExchangeRateLoader.class);
        ExchangeRateScheduler scheduler = new ExchangeRateScheduler(loader);

        scheduler.refreshRates();

        verify(loader).fetchAndLoadAllCurrencyRates();
    }
}
