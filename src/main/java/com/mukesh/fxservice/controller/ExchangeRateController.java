package com.mukesh.fxservice.controller;

import com.mukesh.fxservice.dto.ConversionResponse;
import com.mukesh.fxservice.dto.ExchangeRateResponse;
import com.mukesh.fxservice.service.ExchangeRateService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/currencies")
    public List<String> getCurrencies() {
        return service.getAvailableCurrencies();
    }

    @GetMapping("/rates")
    public List<ExchangeRateResponse> getRates(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page index must be 0 or greater")
            int page,
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "Page size must be 1 or greater")
            int size) {

        Pageable pageable = PageRequest.of(page, size);

        if (date == null) {
            return service.getAllRates(pageable).getContent();
        }

        return service.getRatesByDate(date, pageable).getContent();
    }

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
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return service.convert(currency, amount, date);
    }


}
