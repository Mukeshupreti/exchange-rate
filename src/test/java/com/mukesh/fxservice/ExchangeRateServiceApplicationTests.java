package com.mukesh.fxservice;


import com.mukesh.fxservice.external.BundesbankClient;
import com.mukesh.fxservice.repository.ExchangeRateRepository;
import com.mukesh.fxservice.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository repository;

    @Mock
    private BundesbankClient client;

    @InjectMocks
    private ExchangeRateService service;

    public ExchangeRateServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void contextLoads() {
        assertNotNull(service);
    }
}
