package com.prorocketeers.jakub.country.connector;

import com.prorocketeers.jakub.country.connector.dto.Country;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

public class CountryDataClientImpl implements CountryDataClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String countriesUrl;

    public CountryDataClientImpl(ObjectMapper objectMapper, RestClient restClient, String countriesUrl) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.countriesUrl = countriesUrl;
    }

    @Override
    public List<Country> fetchCountries() {
        var json = restClient.get()
                .uri(countriesUrl)
                .retrieve()
                .body(String.class);
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("Country data source returned no data: " + countriesUrl);
        }
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }
}
