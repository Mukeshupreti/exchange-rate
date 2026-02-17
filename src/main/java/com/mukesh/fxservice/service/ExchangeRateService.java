package com.mukesh.fxservice.service;

import com.mukesh.fxservice.config.CurrencyProperties;
import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.dto.ConversionResponse;
import com.mukesh.fxservice.dto.ExchangeRateResponse;
import com.mukesh.fxservice.exception.RateNotFoundException;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;


@Service
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyProperties currencyProperties;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository, CurrencyProperties currencyProperties) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.currencyProperties = currencyProperties;
    }

    public List<String> getAvailableCurrencies() {
        return currencyProperties.getSupportedCurrencies();
    }

    public Page<ExchangeRateResponse> getAllRates(Pageable pageable) {

        Page<ExchangeRate> page = exchangeRateRepository.findAll(pageable);

        if (page.isEmpty()) {
            throw new RateNotFoundException("No exchange rate data available");
        }

        return page.map(this::toResponse);
    }


    public ExchangeRate getRate(String inputCurrency, LocalDate date) {
        String currency = inputCurrency.toUpperCase().trim();
        return exchangeRateRepository
                .findByCurrencyAndRateDate(currency, date)
                .orElseThrow(() ->
                        new RateNotFoundException("No exchange rate data available for date: " + date));
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
                rate.getRateDate()
        );
    }
    public Page<ExchangeRateResponse> getRatesByDate(LocalDate date, Pageable pageable) {

        Page<ExchangeRate> page = exchangeRateRepository.findByRateDate(date, pageable);

        if (page.isEmpty()) {
            throw new RateNotFoundException(
                    "No exchange rate data available for date: " + date);
        }

        return page.map(this::toResponse);
    }

    private ExchangeRateResponse toResponse(ExchangeRate rate) {
        return new ExchangeRateResponse(
                rate.getCurrency(),
                rate.getRate(),
                rate.getRateDate()
        );
    }


}
