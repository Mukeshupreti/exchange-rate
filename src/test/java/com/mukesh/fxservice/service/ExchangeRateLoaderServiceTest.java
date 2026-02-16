package com.mukesh.fxservice.service;

import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.external.BundesbankClient;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ExchangeRateLoaderServiceTest {

    @MockBean
    private ExchangeRateRepository repository;

    @MockBean
    private BundesbankClient client;

    @Autowired
    private ExchangeRateLoaderService loader;

    @BeforeEach
    void setup() {
        // Spring will provide loader with mocked beans
    }

    @Test
    void fetchAndStoreRates_onlySavesNewDates() {
        String csv = "TIME_PERIOD,OBS_VALUE\n2023-01-01,1.1\n2023-01-02,1.2\n2023-01-03,1.3\n";
        when(client.fetchExchangeRatesCsv("USD")).thenReturn(csv);

        // Simulate repository already has one of the dates
        when(repository.findByCurrencyAndRateDateIn(eq("USD"), any()))
                .thenReturn(List.of(new ExchangeRate("USD", BigDecimal.valueOf(1.1), LocalDate.parse("2023-01-01"))));

        loader.fetchAndStoreRates("USD");

        ArgumentCaptor<List<ExchangeRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository, times(1)).saveAll(captor.capture());

        List<ExchangeRate> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(ExchangeRate::getRateDate)
                .contains(LocalDate.parse("2023-01-02"), LocalDate.parse("2023-01-03"));
    }
}
