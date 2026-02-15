package com.mukesh.fxservice.repository;

import com.mukesh.fxservice.domain.ExchangeRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyAndRateDate(String currency, LocalDate rateDate);

    Page<ExchangeRate> findByRateDate(LocalDate rateDate, Pageable pageable);

    Optional<ExchangeRate>
    findTopByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
            String currency,
            LocalDate rateDate
    );

       List<ExchangeRate> findByCurrencyAndRateDateIn(String currency, Collection<LocalDate> rateDates);

}
