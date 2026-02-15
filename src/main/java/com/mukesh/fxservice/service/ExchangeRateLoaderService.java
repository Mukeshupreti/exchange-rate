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
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Transactional
    public List<ExchangeRate> fetchAndStoreRates(String currency) {
        log.info("fetching exchange data for currency :{}",currency);
        String csvData = client.fetchExchangeRatesCsv(currency);
        List<ExchangeRate> rates = parseCsv(currency, csvData);
        repository.saveAll(rates);
        log.info("fetch done for currency :{}",currency);
        return rates;
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

            Iterable<CSVRecord> records = CSVFormat.DEFAULT
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
                    LocalDate date = LocalDate.parse(dateStr);
                    BigDecimal rate = new BigDecimal(rateStr);

                    rates.add(new ExchangeRate(currency, rate, date));

                } catch (Exception e) {
                    // Skip malformed row safely
                    continue;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV", e);
        }

        return rates;
    }
}
