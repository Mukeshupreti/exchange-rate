package com.mukesh.fxservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConversionResponse(
        String currency,
        BigDecimal originalAmount,
        BigDecimal rate,
        BigDecimal convertedAmount,
        LocalDate rateDate,
        boolean fallbackUsed
) {}
