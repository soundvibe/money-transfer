[![Build Status](https://travis-ci.org/soundvibe/money-transfer.png)](https://travis-ci.org/soundvibe/money-transfer)
[![Coverage Status](https://codecov.io/github/soundvibe/money-transfer/coverage.svg?branch=master)](https://codecov.io/github/soundvibe/money-transfer?branch=master)

# money-transfer-api

Works with Java >= 11

Restful service implementing money transfer functionality.

Starts locally using port `8181`.

[Swagger API documentation](http://localhost:8181) 

Service uses synchronous and asynchronous Restful APIs, uses Micrometer for metrics and logback for logging.
Service also exposes [health-check(liveness) endpoint](http://localhost:8181/health) and [metrics endpoint](http://localhost:8181/metrics)
