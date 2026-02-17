package com.mukesh.fxservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConversionResponse(
        String currency,
        BigDecimal amount,
        BigDecimal rate,
        BigDecimal convertedAmount,
        LocalDate rateDate
) {
}
