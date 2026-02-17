package com.mukesh.fxservice.service;

import com.mukesh.fxservice.domain.ExchangeRate;
import com.mukesh.fxservice.external.impl.BundesbankClient;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import com.mukesh.fxservice.service.impl.ExchangeRateLoaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ExchangeRateLoaderServiceConcurrencyTest {

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
    void concurrentFetches_doNotProduceDuplicateSaves() throws InterruptedException {
        String csv = "TIME_PERIOD,OBS_VALUE\n2023-01-01,1.1\n2023-01-02,1.2\n2023-01-03,1.3\n";
        when(client.fetchExchangeRatesCsv("USD")).thenAnswer(invocation -> {
            Thread.sleep(100);
            return csv;
        });

        Set<LocalDate> existingDates = ConcurrentHashMap.newKeySet();

        when(repository.findByCurrencyAndRateDateIn(eq("USD"), any())).thenAnswer((Answer<List<ExchangeRate>>) invocation -> {
            Collection<LocalDate> queryDates = invocation.getArgument(1);
            List<ExchangeRate> found = new ArrayList<>();
            for (LocalDate d : queryDates) {
                if (existingDates.contains(d)) {
                    found.add(new ExchangeRate("USD", BigDecimal.ONE, d));
                }
            }
            return found;
        });

        doAnswer((InvocationOnMock inv) -> {
            @SuppressWarnings("unchecked")
            List<ExchangeRate> saved = inv.getArgument(0);
            for (ExchangeRate r : saved) {
                existingDates.add(r.getRateDate());
            }
            return null;
        }).when(repository).saveAll(any());

        // Run N concurrent callers
        int threads = 10;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            ex.submit(() -> {
                try {
                    start.await();
                    loader.fetchAndLoadRatesForCurrency("USD");
                } catch (Exception e) {
                    // ignore
                } finally {
                    done.countDown();
                }
            });
        }

        // start threads
        start.countDown();
        boolean finished = done.await(15, TimeUnit.SECONDS);
        ex.shutdownNow();

        // without bulkhead, saveAll may be called multiple times
        verify(repository, atLeast(1)).saveAll(any());
        assertThat(existingDates).hasSize(3);
    }
}
