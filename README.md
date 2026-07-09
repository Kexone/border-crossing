# Border Crossing Service

Spring Boot service that calculates the shortest land route between two
countries using their border information from
[mledoze/countries](https://github.com/mledoze/countries).

## Modules

| Module | Purpose |
|---|---|
| `country-connector` | Client for the country data source (`CountryDataClient`), reusable as a standalone library |
| `border-crossing-backend` | Business logic: border graph, route search, scheduled data refresh |
| `border-crossing-app` | REST layer: OpenAPI contract, generated API, controller, error handling; the executable Spring Boot application |

## Requirements

- Java 25 — no local Maven needed, the Maven wrapper (`mvnw`) is included
- or just Docker (see below)

## Build & test

```bash
./mvnw clean verify
```

## Run

```bash
./mvnw -pl border-crossing-app spring-boot:run
```

or run the packaged jar:

```bash
java -jar border-crossing-app/target/border-crossing-app-0.0.1-SNAPSHOT.jar
```

The application downloads the country data on startup (network access
required), refreshes it every 24 hours and listens on port 8080.

Configuration (`border-crossing-app/src/main/resources/application.yaml`):

| Property | Default | Meaning |
|---|---|---|
| `countries.data.url` | mledoze/countries JSON | Country data source |
| `countries.data.connect-timeout` | `5s` | HTTP connect timeout for the data source |
| `countries.data.read-timeout` | `30s` | HTTP read timeout for the data source |
| `countries.cache.ttl` | `PT24H` | How often the border graph is refreshed |

## Run with Docker

```bash
docker build -t border-crossing .
docker run -p 8080:8080 border-crossing
```

## API

The API is designed contract-first: the OpenAPI document at
`border-crossing-app/src/main/resources/openapi/v1/border-crossing.yaml` is the
source of truth, and the server interface plus models are generated from it
during the build (`openapi-generator-maven-plugin`).

```
GET /routing/{origin}/{destination}
```

Countries are identified by their `cca3` code (case-insensitive). Returns the
shortest list of border crossings from origin to destination:

```bash
curl http://localhost:8080/routing/CZE/ITA
```

```json
{ "route": ["CZE", "AUT", "ITA"] }
```

Errors are returned as a `code` + `message` pair. `400 Bad Request` is used
when a country code is malformed (`INVALID_COUNTRY_CODE`) or when no land
route exists (`NO_LAND_ROUTE`); unexpected failures return `500` with
`INTERNAL_ERROR`:

```json
{ "code": "NO_LAND_ROUTE", "message": "No land route from CZE to AUS" }
```

Interactive documentation is available at
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
(OpenAPI spec at `/v3/api-docs`).

## Design notes

- The border data is cached in memory as an adjacency map and rebuilt on a
  schedule by a dedicated scheduler component; if a refresh fails, the service
  keeps serving the last good data.
- Routes are found with a bidirectional BFS (two search fronts meeting in the
  middle), which explores roughly O(b^(d/2)) countries instead of O(b^d) for a
  plain BFS.
- Country codes are validated against the ISO 3166-1 alpha-3 format declared
  in the OpenAPI contract; validation and business errors are rendered
  consistently by a global `@RestControllerAdvice` exception handler using the
  generated `ErrorResponse` model, so error bodies cannot drift from the
  contract.
- The connector module wires itself via a `@Configuration` class imported
  explicitly by the application, and its HTTP client has explicit connect and
  read timeouts.
