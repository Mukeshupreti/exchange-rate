package com.mukesh.fxservice.repository;

import com.mukesh.fxservice.domain.ExchangeRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyAndRateDate(String currency, LocalDate rateDate);
    Page<ExchangeRate> findByRateDate(LocalDate rateDate, Pageable pageable);

    @Query("SELECT DISTINCT e.currency FROM ExchangeRate e")
    List<String> findDistinctCurrencies();

    // 🔥 Add this fallback method
    Optional<ExchangeRate>
    findTopByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
            String currency,
            LocalDate rateDate
    );



}
