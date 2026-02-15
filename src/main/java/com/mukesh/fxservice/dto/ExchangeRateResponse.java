package com.mukesh.fxservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateResponse(
        String currency,
        BigDecimal rate,
        LocalDate rateDate
) {
}
