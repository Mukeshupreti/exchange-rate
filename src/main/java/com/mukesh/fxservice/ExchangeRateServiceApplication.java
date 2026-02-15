package com.mukesh.fxservice;

import com.mukesh.fxservice.config.CurrencyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(CurrencyProperties.class)
@EnableScheduling
public class ExchangeRateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExchangeRateServiceApplication.class, args);
    }

}
