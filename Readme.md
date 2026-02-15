application url
http://localhost:8080/exchange-rate-service/actuator/health
http://localhost:8080/actuator/health
h2 db


As a client, I want to get a list of all available currencies
GET /api/currencies

As a client, I want to get all EUR-FX exchange rates 	
GET  /api/rates

As a client, I want to get the EUR-FX exchange rate at particular day
GET /api/rates?date=2024-01-10

As a client, I want to get a foreign exchange amount for a given currency converted to EUR on a particular day
GET /exchange/ {fromcurreny}/{tocurrecy}? desiredDate=
GET /api/conversions?currency=USD&amount=100&date=2024-01-10



