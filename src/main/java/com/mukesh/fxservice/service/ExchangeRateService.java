package com.mukesh.fxservice.service;

import com.mukesh.fxservice.config.CurrencyProperties;
import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.dto.ConversionResponse;
import com.mukesh.fxservice.dto.ExchangeRateResponse;
import com.mukesh.fxservice.exception.RateNotFoundException;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;


@Service
public class ExchangeRateService {

    private static final Logger log =
            LoggerFactory.getLogger(ExchangeRateService.class);
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateLoaderService loader;
    private final CurrencyProperties currencyProperties;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository, ExchangeRateLoaderService loader, CurrencyProperties currencyProperties) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.loader = loader;
        this.currencyProperties = currencyProperties;
    }

    @CircuitBreaker(name = "bundesbank", fallbackMethod = "fallbackRates")
    @Retry(name = "bundesbank")
    public List<ExchangeRate> fetchAndStoreRates(String currency) {
        log.info("Fetching exchange rates from Bundesbank | currency={}", currency);
        currency = currency.toUpperCase().trim();

        List<ExchangeRate> rates = loader.fetchAndStoreRates(currency);
        log.info("Successfully stored {} rates | currency={}",
                rates.size(), currency);

        return rates;
    }

    public List<ExchangeRate> fallbackRates(String currency, Throwable throwable) {

        log.warn("Bundesbank unavailable for {}. Returning empty list.", currency);

        return Collections.emptyList();
    }


    public List<String> getAvailableCurrencies() {
        return currencyProperties.getSupportedCurrencies();
    }

    public Page<ExchangeRateResponse> getAllRates(Pageable pageable) {

        Page<ExchangeRate> page = exchangeRateRepository.findAll(pageable);

        if (page.isEmpty()) {

            log.info("Database empty. Performing initial load.");

            loader.loadAllSupportedCurrencies();

            page = exchangeRateRepository.findAll(pageable);
        }

        if (page.isEmpty()) {
            throw new RateNotFoundException("No exchange rate data available");
        }

        return page.map(this::toResponse);
    }


    public ExchangeRate getRate(String inputCurrency, LocalDate date) {
        String currency = inputCurrency.toUpperCase().trim();
        // 1️⃣ Try exact match in DB
        return exchangeRateRepository
                .findByCurrencyAndRateDate(currency, date)
                .orElseGet(() -> {

                    try {
                        // 2️⃣ Try refresh from Bundesbank
                        fetchAndStoreRates(currency);

                        return exchangeRateRepository
                                .findByCurrencyAndRateDate(currency, date)
                                .orElseThrow();
                    } catch (Exception ex) {

                        // 3️⃣ Fallback to latest available rate
                        return exchangeRateRepository
                                .findTopByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                                        currency, date)
                                .orElseThrow(() ->
                                        new RateNotFoundException("No fallback rate available"));
                    }
                });
    }

    public ConversionResponse convert(String inputCurrency,
                                      BigDecimal amount,
                                      LocalDate date) {

        String currency = inputCurrency.toUpperCase().trim();

        ExchangeRate rate = getRate(currency, date);

        if (rate.getRate().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Exchange rate cannot be zero");
        }

        BigDecimal converted =
                amount.divide(rate.getRate(), 6, RoundingMode.HALF_UP);

        return new ConversionResponse(
                currency,
                amount,
                rate.getRate(),
                converted,
                rate.getRateDate(),
                !rate.getRateDate().equals(date)   // fallbackUsed
        );
    }

    private ExchangeRateResponse toResponse(ExchangeRate rate) {
        return new ExchangeRateResponse(
                rate.getCurrency(),
                rate.getRate(),
                rate.getRateDate()
        );
    }

    public Page<ExchangeRateResponse> getRatesByDate(LocalDate date, Pageable pageable) {

        Page<ExchangeRate> page = exchangeRateRepository.findByRateDate(date, pageable);

        // Lazy load if DB has no data for that date
        if (page.isEmpty()) {

            log.info("No rates found for date {}. Triggering refresh.", date);

            loader.loadAllSupportedCurrencies();

            page = exchangeRateRepository.findByRateDate(date, pageable);
        }

        if (page.isEmpty()) {
            throw new RateNotFoundException(
                    "No exchange rate data available for date: " + date);
        }

        return page.map(this::toResponse);
    }

}
