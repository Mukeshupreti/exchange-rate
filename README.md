# Exchange Rate Service

A Spring Boot application that provides EUR foreign exchange reference rates using Bundesbank data. The application
stores exchange rates in a local database.

A scheduled cron job refreshes the data daily, based on the assumption that exchange rates are updated once per day.

The API is read-only against the local database. Exchange rates are refreshed by the scheduled job.

------------------------------------------------------------------------

## Challenge Brief

This project implements the Crewmeister coding challenge:

- As a client, I want to get a list of all available currencies
- As a client, I want to get all EUR-FX exchange rates at all available dates as a collection
- As a client, I want to get the EUR-FX exchange rate at particular day
- As a client, I want to get a foreign exchange amount for a given currency converted to EUR on a particular day

Bundesbank reference data: https://api.statistiken.bundesbank.de/rest/download/BBEX3

------------------------------------------------------------------------

## Setup

Requirements: Java 21, Maven 3.x

Run:

```bash
mvn spring-boot:run
```

Base URL:

- http://localhost:8080/exchange-rate-service

Swagger UI:

- http://localhost:8080/exchange-rate-service/swagger-ui.html

Actuator:

- http://localhost:8080/exchange-rate-service/actuator/health
- http://localhost:8080/exchange-rate-service/actuator/metrics

------------------------------------------------------------------------

## API Endpoints

- Currency list: GET http://localhost:8080/exchange-rate-service/api/currencies
- All rates (paged): GET http://localhost:8080/exchange-rate-service/api/rates?page=0&size=50
- Rates by date: GET http://localhost:8080/exchange-rate-service/api/rates?date=2024-01-10&page=0&size=50
- Convert to EUR: GET http://localhost:8080/exchange-rate-service/api/conversions?currency=USD&amount=100&date=2024-01-10

------------------------------------------------------------------------

## Architecture Overview

The application follows a layered architecture:



```
Client
  |
  v
Controller
  |
  v
Service
  |
  v
Loader
  |
  +--> External Client (Bundesbank)
  |
  v
Repository
  |
  v
H2 Database
```

### Components:

- ExchangeRateController -- REST endpoints
- ExchangeRateService -- Business logic
- ExchangeRateLoaderService -- Fetch + parse + persist CSV
- BundesbankClient -- External REST client
- ExchangeRateRepository -- JPA repository
- H2 Database -- Local storage

------------------------------------------------------------------------

## Data Refresh Strategy (Cron Job)

The application includes a **scheduled cron job** that automatically
refreshes exchange rate data.

Behavior:

- Read endpoints never call the external API.
- External refresh happens only through the scheduled job.

### How it works:

- A scheduler runs at configured intervals (e.g.daily).
- It loops through all supported currencies.
- It fetches latest rates from Bundesbank.
- It updates the database with new rate rows only.

Example (conceptual):

``` java
@Scheduled(cron = "0 0 3 * * ?")
public void refreshRatesDaily() {
    currencyProperties.getSupportedCurrencies()
        .forEach(loader::fetchAndLoadRatesForCurrency);
}
```

This ensures production readiness and prevents data staleness.

------------------------------------------------------------------------

## Testing

- Unit tests (Parser, Persister, Service)
- Integration tests with MockMvc
- Validation tests
- Negative scenario testing

------------------------------------------------------------------------

## Notes

- Reads never call the external API.
- Refresh happens only through the scheduled job.
