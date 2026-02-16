package com.mukesh.fxservice.service;

import com.mukesh.fxservice.config.CurrencyProperties;
import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.external.BundesbankClient;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExchangeRateLoaderService {

    private static final Logger log =
            LoggerFactory.getLogger(ExchangeRateLoaderService.class);

    private final ExchangeRateRepository repository;
    private final BundesbankClient client;
    private final CurrencyProperties currencyProperties;

    public ExchangeRateLoaderService(
            ExchangeRateRepository repository,
            BundesbankClient client,
            CurrencyProperties currencyProperties) {
        this.repository = repository;
        this.client = client;
        this.currencyProperties = currencyProperties;
    }

    @Bulkhead(name = "bundesbank", type = Bulkhead.Type.SEMAPHORE)
    public List<ExchangeRate> fetchAndStoreRates(String currency) {
        log.info("fetching exchange data for currency :{}", currency);

        try {
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

            return saveUniqueRates(currency, parsed);

        } catch (BulkheadFullException bfe) {
            log.warn("Bulkhead full for {}. Another load may be in progress.", currency);
            return List.of();
        }
    }

    public void loadAllSupportedCurrencies() {
        currencyProperties.getSupportedCurrencies()
                .forEach(this::fetchAndStoreRates);
    }

    @Transactional
    private List<ExchangeRate> saveUniqueRates(String currency, List<ExchangeRate> parsed) {
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

        return toSave;
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

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build();

            Iterable<CSVRecord> records = format.parse(new StringReader(csvData));

            for (CSVRecord record : records) {

                long recordNumber = record.getRecordNumber();

                // Skip header or invalid rows
                if (record.size() < 2) {
                    log.debug("Skipping short/malformed CSV row #{}: {}", recordNumber, record);
                    continue;
                }

                String dateStr = record.get(0).trim();
                String rateStr = record.get(1).trim();

                // Skip header line
                if (dateStr.equalsIgnoreCase("TIME_PERIOD") || dateStr.isBlank()) {
                    // header or blank
                    log.debug("Skipping header/blank CSV row #{}", recordNumber);
                    continue;
                }

                // Skip missing values
                if (rateStr.equals(".") || rateStr.isBlank()) {
                    log.debug("Skipping missing rate value CSV row #{}: {}", recordNumber, record);
                    continue;
                }

                try {
                    // Handle European decimal commas and non-breaking spaces
                    rateStr = rateStr.replace("\u00A0", "").replace(',', '.');

                    LocalDate date = LocalDate.parse(dateStr);
                    BigDecimal rate = new BigDecimal(rateStr);

                    rates.add(new ExchangeRate(currency, rate, date));

                } catch (Exception e) {
                    // Log malformed row and continue
                    log.warn("Skipping malformed CSV row #{} due to parse error: {} - {}", recordNumber, record, e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV", e);
        }

        return rates;
    }
}
