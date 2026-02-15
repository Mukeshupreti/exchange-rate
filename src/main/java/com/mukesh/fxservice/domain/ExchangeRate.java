package com.mukesh.fxservice.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "exchange_rates",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"currency", "rate_date"})
        },
        indexes = {
                @Index(name = "idx_currency_date", columnList = "currency, rate_date")
        }
)
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    protected ExchangeRate() {
    }

    public ExchangeRate(String currency, BigDecimal rate, LocalDate rateDate) {
        this.currency = currency.toUpperCase();
        this.rate = rate;
        this.rateDate = rateDate;
    }

    public Long getId() {
        return id;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public LocalDate getRateDate() {
        return rateDate;
    }
}
