# Exchange Rate Service

A Spring Boot application that provides EUR foreign exchange reference rates using Bundesbank data. The application
stores exchange rates in a local database to avoid calling the external Bundesbank API on every request.

A scheduled cron job refreshes the data daily, based on the assumption that exchange rates are updated once per day.

Additionally, the API supports lazy loading — if requested data is not available in the database, it automatically
fetches the required rates from the Bundesbank API and stores them for future use.

------------------------------------------------------------------------

## Application URLs

Application
URL: http://localhost:8080

Swagger UI
URL: http://localhost:8080/swagger-ui.html

Health Check
URL: http://localhost:8080/actuator/health

Metrics
URL: http://localhost:8080/actuator/metrics

Purpose: H2 Console
URL: http://localhost:8080/h2-console

------------------------------------------------------------------------

## Main Features

### List of all available currencies

GET /api/currencies

### Get all exchange rates (paginated)

GET /api/rates?page=0&size=50

### Get all exchange rates for a specific date

GET /api/rates?date=2024-01-10&page=0&size=50

### Convert currency to EUR

GET /api/conversions?currency=USD&amount=100&date=2024-01-10

------------------------------------------------------------------------

## Architecture Overview

The application follows a layered architecture:

Controller → Service → Loader → External Client → Repository → Database

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

### Why?

To ensure: - Database remains up-to-date - No manual reload required -
Fresh rates available daily - System resilient against missing data

### How it works:

- A scheduler runs at configured intervals (e.g.daily).
- It loops through all supported currencies.
- It fetches latest rates from Bundesbank.
- It updates the database using UPSERT logic.

Example (conceptual):

``` java
@Scheduled(cron = "0 0 3 * * ?")
public void refreshRatesDaily() {
    currencyProperties.getSupportedCurrencies()
        .forEach(loader::fetchAndStoreRates);
}
```

This ensures production readiness and prevents data staleness.

------------------------------------------------------------------------

## Resilience & Reliability

- Resilience4j Circuit Breaker
- Retry mechanism
- Fallback to latest available rate
- Global exception handling
- Validation at API level
- Bulkhead for external calls

------------------------------------------------------------------------

## Monitoring & Observability

- Spring Boot Actuator enabled
- Health endpoint exposed
- Metrics endpoint available
- HikariCP connection pool monitoring

------------------------------------------------------------------------

## Database Strategy

Development: - H2 File-based database

------------------------------------------------------------------------

## Assumptions Made

1. Rates are stored historically (no deletion).
2. Daily refresh is sufficient for business needs.
3. Pagination required to prevent huge api response in case of get all rates.
4. Currency list configurable via properties file (collected from Bundesbank).

------------------------------------------------------------------------

## Testing

- Unit tests (Controller & Service)
- Integration tests with MockMvc
- Validation tests
- Negative scenario testing

------------------------------------------------------------------------

## Design Decisions

- Loader service separated for transactional integrity.
- Lazy load implemented when DB empty.
- Pagination enforced for performance.
- DTO layer added to avoid entity exposure.
- Global exception handler for consistent API errors.

------------------------------------------------------------------------

## Future Improvements

- Redis caching layer
- Dockerization
- CI/CD pipeline
- Rate limiting
- API authentication (JWT)

------------------------------------------------------------------------

