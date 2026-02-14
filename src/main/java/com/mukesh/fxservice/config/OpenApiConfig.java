package com.mukesh.fxservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI exchangeRateApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Exchange Rate Service API")
                        .description("Foreign exchange rate service using Bundesbank reference data")
                        .version("1.0.0"));
    }
}
