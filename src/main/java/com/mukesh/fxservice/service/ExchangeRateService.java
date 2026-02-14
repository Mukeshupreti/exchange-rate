package com.mukesh.fxservice.service;

import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.dto.ConversionResponse;
import com.mukesh.fxservice.external.BundesbankClient;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Service
public class ExchangeRateService {

    private final BundesbankClient bundesbankClient;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateLoaderService loader;


    private static final Logger log =
            LoggerFactory.getLogger(ExchangeRateService.class);


    public ExchangeRateService(BundesbankClient bundesbankClient,
                               ExchangeRateRepository exchangeRateRepository,ExchangeRateLoaderService loader) {
        this.bundesbankClient = bundesbankClient;
        this.exchangeRateRepository = exchangeRateRepository;
        this.loader=loader;
    }

    // ==============================
    // Fetch from Bundesbank and store
    // ==============================
    @CircuitBreaker(name = "bundesbank", fallbackMethod = "fallbackRates")
    @Retry(name = "bundesbank")
    @Transactional
    public List<ExchangeRate> fetchAndStoreRates(String currency) {
        log.info("Fetching exchange rates from Bundesbank | currency={}", currency);
        currency = currency.toUpperCase().trim();

        List<ExchangeRate> rates = loader.fetchAndStoreRates(currency);;
        log.info("Successfully stored {} rates | currency={}",
                rates.size(), currency);

        return rates;
    }

    public List<ExchangeRate> fallbackRates(String currency, Throwable throwable) {

        log.warn("Bundesbank unavailable for {}. Returning empty list.", currency);

        return Collections.emptyList();
    }


    // ==============================
    // Get Available Currencies
    // ==============================

    public List<String> getAvailableCurrencies() {
        return exchangeRateRepository.findAll()
                .stream()
                .map(ExchangeRate::getCurrency)
                .distinct()
                .toList();
    }

    // ==============================
    // Get rates by date
    // ==============================

    public List<ExchangeRate> getRatesByDate(LocalDate date) {

        List<ExchangeRate> rates = exchangeRateRepository.findByRateDate(date);

        if (rates.isEmpty()) {
            throw new RuntimeException("No data available for date: " + date);
        }

        return rates;
    }

    // ==============================
    // Get specific rate
    // ==============================

    public ExchangeRate getRate(String inputCurrency, LocalDate date) {
      String  currency = inputCurrency.toUpperCase().trim();
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
                                        new RuntimeException(
                                                "No fallback rate available"));
                    }
                });
    }



    // ==============================
    // Convert to EUR
    // ==============================

     public ConversionResponse convert(String currency,
                                      BigDecimal amount,
                                      LocalDate date) {

        boolean fallbackUsed = false;

        ExchangeRate rate;

        try {
            rate = getRate(currency, date);
        } catch (Exception ex) {

            // fallback already handled inside getRate
            fallbackUsed = true;
            rate = exchangeRateRepository
                    .findTopByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(currency, date)
                    .orElseThrow(() -> new RuntimeException("No rate available"));
        }

        BigDecimal converted =
                amount.divide(rate.getRate(), 6, RoundingMode.HALF_UP);

        return new ConversionResponse(
                currency,
                amount,
                rate.getRate(),
                converted,
                rate.getRateDate(),
                fallbackUsed
        );
    }




    // ==============================
    // CSV Parsing
    // ==============================



}
