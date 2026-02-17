package com.mukesh.fxservice.service;

import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.external.impl.BundesbankClient;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import com.mukesh.fxservice.service.impl.ExchangeRateLoaderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ExchangeRateLoaderServiceTest {

    @MockBean
    private ExchangeRateRepository repository;

    @MockBean
    private BundesbankClient client;

    @Autowired
    private ExchangeRateLoaderService loader;

    @Test
    void fetchAndStoreRates_onlySavesNewDates() {
        String csv = "TIME_PERIOD,OBS_VALUE\n2023-01-01,1.1\n2023-01-02,1.2\n2023-01-03,1.3\n";
        when(client.fetchExchangeRatesCsv("USD")).thenReturn(csv);

        when(repository.findByCurrencyAndRateDateIn(eq("USD"), any()))
                .thenReturn(List.of(new ExchangeRate("USD", BigDecimal.valueOf(1.1), LocalDate.parse("2023-01-01"))));

        loader.fetchAndLoadRatesForCurrency("USD");

        verify(repository, times(1)).saveAll(argThat(saved ->
                StreamSupport.stream(saved.spliterator(), false).count() == 2
                        && StreamSupport.stream(saved.spliterator(), false)
                                .anyMatch(rate -> rate.getRateDate().equals(LocalDate.parse("2023-01-02")))
                        && StreamSupport.stream(saved.spliterator(), false)
                                .anyMatch(rate -> rate.getRateDate().equals(LocalDate.parse("2023-01-03")))
        ));
    }
}
