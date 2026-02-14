package com.mukesh.fxservice.external;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BundesbankClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String formatSuffix;

    public BundesbankClient(
            RestTemplate restTemplate,
            @Value("${bundesbank.base-url}") String baseUrl,
            @Value("${bundesbank.format-suffix}") String formatSuffix) {

        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.formatSuffix = formatSuffix;
    }

    public String fetchExchangeRatesCsv(String currency) {

        String url = String.format(
                "%s/D.%s.%s",
                baseUrl,
                currency,
                formatSuffix
        );

        return restTemplate.getForObject(url, String.class);
    }
}
