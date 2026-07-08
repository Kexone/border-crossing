# Border Crossing Service

Spring Boot service that calculates the shortest land route between two
countries using their border information from
[mledoze/countries](https://github.com/mledoze/countries).

## Requirements

- Java 17+ — no local Maven needed, the Maven wrapper (`mvnw`) is included
- or just Docker (see below)

## Build & test

```bash
./mvnw clean verify
```

## Run

```bash
./mvnw spring-boot:run
```

or run the packaged jar:

```bash
java -jar target/border-crossing-0.0.1-SNAPSHOT.jar
```

The application downloads the country data on startup (network access
required), refreshes it every 24 hours (configurable via `countries.cache.ttl`
in `application.properties`) and listens on port 8080.

## Run with Docker

```bash
docker build -t border-crossing .
docker run -p 8080:8080 border-crossing
```

## API

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

When no land route exists or a country code is unknown, the service returns
`400 Bad Request` as an RFC 9457 problem detail:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "No land route from CZE to AUS"
}
```

Interactive documentation is available at
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
(OpenAPI spec at `/v3/api-docs`).

## Design notes

- The border data is cached in memory as an adjacency map and rebuilt on a
  schedule; if a refresh fails, the service keeps serving the last good data.
- Routes are found with a bidirectional BFS, which explores roughly
  O(b^(d/2)) countries instead of O(b^d) for a plain BFS.
- Errors are rendered consistently as `application/problem+json` by a global
  `@RestControllerAdvice` exception handler.
