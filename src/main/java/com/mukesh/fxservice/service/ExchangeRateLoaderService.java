package com.mukesh.fxservice.service;

import com.mukesh.fxservice.config.CurrencyProperties;
import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.external.BundesbankClient;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class ExchangeRateLoaderService {

    private static final Logger log =
            LoggerFactory.getLogger(ExchangeRateLoaderService.class);

    private final ExchangeRateRepository repository;
    private final BundesbankClient client;
    private final CurrencyProperties currencyProperties;

    // per-currency locks to serialize loads for the same currency
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ExchangeRateLoaderService(
            ExchangeRateRepository repository,
            BundesbankClient client,
            CurrencyProperties currencyProperties) {
        this.repository = repository;
        this.client = client;
        this.currencyProperties = currencyProperties;
    }

    @Transactional
    public List<ExchangeRate> fetchAndStoreRates(String currency) {
        log.info("fetching exchange data for currency :{}", currency);

        ReentrantLock lock = locks.computeIfAbsent(currency, k -> new ReentrantLock());
        boolean acquired = false;
        try {
            // wait up to 15 seconds to acquire lock; if not acquired, avoid hammering
            acquired = lock.tryLock(15, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Could not acquire load lock for {}. Another load may be in progress.", currency);
                return List.of();
            }

            String csvData = client.fetchExchangeRatesCsv(currency);
            if (csvData == null || csvData.isBlank()) {
                log.warn("Received empty CSV for currency {}", currency);
                return List.of();
            }

            List<ExchangeRate> parsed = parseCsv(currency, csvData);
            if (parsed.isEmpty()) {
                log.info("No rates parsed for currency {}", currency);
                return List.of();
            }

            // deduplicate: find existing rates in DB for the parsed dates
            Set<LocalDate> parsedDates = parsed.stream()
                    .map(ExchangeRate::getRateDate)
                    .collect(Collectors.toSet());

            List<ExchangeRate> existing = repository.findByCurrencyAndRateDateIn(currency, parsedDates);

            Set<LocalDate> existingDates = existing.stream()
                    .map(ExchangeRate::getRateDate)
                    .collect(Collectors.toSet());

            List<ExchangeRate> toSave = parsed.stream()
                    .filter(r -> !existingDates.contains(r.getRateDate()))
                    .collect(Collectors.toList());

            if (toSave.isEmpty()) {
                log.info("All parsed rates already exist for currency {}", currency);
                return parsed;
            }

            try {
                repository.saveAll(toSave);
                log.info("Saved {} new rates for currency {}", toSave.size(), currency);
            } catch (DataIntegrityViolationException dive) {
                // Last-resort: unique constraint might have been violated concurrently
                log.warn("DataIntegrityViolation while saving rates for {}. Some rows may already exist. Error: {}", currency, dive.getMessage());
                // swallow to keep operation idempotent
            }

            log.info("fetch done for currency :{}", currency);
            return toSave;

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for currency load lock", ie);
        } finally {
            if (acquired) {
                lock.unlock();
            }
            // avoid memory leak: remove lock if no longer used
            if (!lock.isLocked()) {
                locks.remove(currency, lock);
            }
        }
    }

    public void loadAllSupportedCurrencies() {
        currencyProperties.getSupportedCurrencies()
                .forEach(this::fetchAndStoreRates);
    }

    private List<ExchangeRate> parseCsv(String currency, String csvData) {

        List<ExchangeRate> rates = new ArrayList<>();

        try {

            // Remove UTF-8 BOM if present
            if (csvData.startsWith("\uFEFF")) {
                csvData = csvData.substring(1);
            }

            // Detect delimiter (comma or semicolon) by checking first few lines
            char delimiter = ',';
            String firstLine = csvData.split("\r?\n", 2)[0];
            if (firstLine.contains(";")) {
                delimiter = ';';
            }

            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withDelimiter(delimiter)
                    .withIgnoreEmptyLines()
                    .withTrim()
                    .parse(new StringReader(csvData));

            for (CSVRecord record : records) {

                // Skip header or invalid rows
                if (record.size() < 2) {
                    continue;
                }

                String dateStr = record.get(0).trim();
                String rateStr = record.get(1).trim();

                // Skip header line
                if (dateStr.equalsIgnoreCase("TIME_PERIOD")
                        || dateStr.isBlank()) {
                    continue;
                }

                // Skip missing values
                if (rateStr.equals(".") || rateStr.isBlank()) {
                    continue;
                }

                try {
                    // Handle European decimal commas
                    rateStr = rateStr.replace("\u00A0", "").replace(',', '.');

                    LocalDate date = LocalDate.parse(dateStr);
                    BigDecimal rate = new BigDecimal(rateStr);

                    rates.add(new ExchangeRate(currency, rate, date));

                } catch (Exception e) {
                    // Log malformed row and continue
                    log.debug("Skipping malformed CSV row: {}", record);
                    continue;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV", e);
        }

        return rates;
    }
}
