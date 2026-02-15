package com.mukesh.fxservice.external;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class BundesbankClient {

    private static final Logger log = LoggerFactory.getLogger(BundesbankClient.class);

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

    @RateLimiter(name = "bundesbank")
    @Retry(name = "bundesbank")
    public String fetchExchangeRatesCsv(String currency) {

        String url = String.format(
                "%s/D.%s.%s",
                baseUrl,
                currency,
                formatSuffix
        );

        log.debug("Requesting Bundesbank CSV: {}", url);

        try {
            String resp = restTemplate.getForObject(url, String.class);
            if (resp == null) {
                throw new RestClientException("Empty response from Bundesbank for url=" + url);
            }
            return resp;
        } catch (RestClientException ex) {
            log.error("Error fetching data from Bundesbank: {}", ex.getMessage());
            throw ex;
        }
    }
}
