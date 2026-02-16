package com.mukesh.fxservice.scheduler;

import com.mukesh.fxservice.service.ExchangeRateLoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(ExchangeRateScheduler.class);

    private final ExchangeRateLoaderService loader;

    public ExchangeRateScheduler(ExchangeRateLoaderService loader) {
        this.loader = loader;
    }

    @Scheduled(cron = "${app.fx.refresh-cron}")
    public void refreshRates() {

        log.info("Starting scheduled FX refresh");
        loader.loadAllSupportedCurrencies();
        log.info("Completed scheduled FX refresh");
    }
}
