package com.mukesh.fxservice.controller;

import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.dto.ConversionResponse;
import com.mukesh.fxservice.service.ExchangeRateService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class ExchangeRateController {

    private final ExchangeRateService service;

    public ExchangeRateController(ExchangeRateService service) {
        this.service = service;
    }

    // 1️⃣ List currencies
    @GetMapping("/currencies")
    public List<String> getCurrencies() {
        return service.getAvailableCurrencies();
    }

    // 2️⃣ All rates or by date
    @GetMapping("/rates")
    public List<ExchangeRate> getRates(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        if (date == null) {
            return service.getRatesByDate(LocalDate.now());
        }

        return service.getRatesByDate(date);
    }

    // 3️⃣ Conversion
    @GetMapping("/conversions")
    public ConversionResponse convert(
            @RequestParam
            @NotBlank(message = "Currency must not be blank")
            @Pattern(regexp = "^[A-Z]{3}$",
                    message = "Currency must be 3 uppercase letters")
            String currency,

            @RequestParam
            @Positive(message = "Amount must be positive")
            BigDecimal amount,

            @RequestParam
            @NotNull(message = "Date must not be null")
            LocalDate date
    ) {
        return service.convert(currency, amount, date);
    }

/*
    @GetMapping("/load/{currency}")
    public String load(@PathVariable String currency) {
        service.fetchAndStoreRates(currency);
        return "Loaded";
    }
*/

}
