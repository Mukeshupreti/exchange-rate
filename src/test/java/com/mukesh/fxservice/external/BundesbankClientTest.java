package com.mukesh.fxservice.external;

import com.mukesh.fxservice.external.impl.BundesbankClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BundesbankClientTest {

    @Test
    void fetchExchangeRatesCsv_successfulResponse() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        String base = "https://api.test";
        String suffix = "EUR.BB.AC.000?format=csv&lang=en";
        BundesbankClient client = new BundesbankClient(restTemplate, base, suffix);

        String expectedUrl = String.format("%s/D.%s.%s", base, "USD", suffix);
        when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn("a,b,c\n");

        String resp = client.fetchExchangeRatesCsv("USD");
        assertThat(resp).isNotNull();
        verify(restTemplate, times(1)).getForObject(expectedUrl, String.class);
    }

    @Test
    void fetchExchangeRatesCsv_nullResponse_throws() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        String base = "https://api.test";
        String suffix = "EUR.BB.AC.000?format=csv&lang=en";
        BundesbankClient client = new BundesbankClient(restTemplate, base, suffix);

        String expectedUrl = String.format("%s/D.%s.%s", base, "USD", suffix);
        when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn(null);

        assertThrows(RestClientException.class, () -> client.fetchExchangeRatesCsv("USD"));
        verify(restTemplate, times(1)).getForObject(expectedUrl, String.class);
    }

    @Test
    void fetchExchangeRatesCsv_restClientException_propagates() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        String base = "https://api.test";
        String suffix = "EUR.BB.AC.000?format=csv&lang=en";
        BundesbankClient client = new BundesbankClient(restTemplate, base, suffix);

        String expectedUrl = String.format("%s/D.%s.%s", base, "USD", suffix);
        when(restTemplate.getForObject(expectedUrl, String.class)).thenThrow(new RestClientException("down"));

        assertThrows(RestClientException.class, () -> client.fetchExchangeRatesCsv("USD"));
        verify(restTemplate, times(1)).getForObject(expectedUrl, String.class);
    }
}
