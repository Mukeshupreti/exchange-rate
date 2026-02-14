package com.mukesh.fxservice.repository;

import com.mukesh.fxservice.domain.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyAndRateDate(String currency, LocalDate rateDate);

    List<ExchangeRate> findByRateDate(LocalDate rateDate);

    List<ExchangeRate> findByCurrency(String currency);


    // 🔥 Add this fallback method
    Optional<ExchangeRate>
    findTopByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
            String currency,
            LocalDate rateDate
    );

}
