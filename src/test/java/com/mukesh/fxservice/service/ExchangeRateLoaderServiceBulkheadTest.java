package com.mukesh.fxservice.service;

import com.mukesh.fxservice.external.BundesbankClient;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "resilience4j.bulkhead.instances.bundesbank.maxConcurrentCalls=1",
        "resilience4j.bulkhead.instances.bundesbank.maxWaitDuration=0ms"
})
public class ExchangeRateLoaderServiceBulkheadTest {

    @MockBean
    private ExchangeRateRepository repository;

    @MockBean
    private BundesbankClient client;

    @Autowired
    private ExchangeRateLoaderService loader;

    @Test
    void bulkhead_allowsOnlyOneConcurrentClientCall() throws InterruptedException {
        String csv = "TIME_PERIOD,OBS_VALUE\n2023-01-01,1.1\n";

        // First call will sleep to simulate long-running fetch; others should be rejected immediately
        when(client.fetchExchangeRatesCsv("USD")).thenAnswer(invocation -> {
            Thread.sleep(500);
            return csv;
        });

        when(repository.findByCurrencyAndRateDateIn(eq("USD"), any()))
                .thenReturn(Collections.emptyList());

        int threads = 10;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            ex.submit(() -> {
                try {
                    start.await();
                    loader.fetchAndStoreRates("USD");
                } catch (Exception e) {
                    // ignore
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean finished = done.await(10, TimeUnit.SECONDS);
        ex.shutdownNow();

        // Verify the external client was called at most once due to bulkhead
        verify(client, atMost(1)).fetchExchangeRatesCsv("USD");
    }
}

