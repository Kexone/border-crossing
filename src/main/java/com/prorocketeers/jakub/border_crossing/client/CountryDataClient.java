package com.prorocketeers.jakub.border_crossing.client;

import com.prorocketeers.jakub.border_crossing.dto.Country;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class CountryDataClient {

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String countriesUrl;

	public CountryDataClient(ObjectMapper objectMapper,
			@Value("${countries.data.url}") String countriesUrl) {
		this.restClient = RestClient.create();
		this.objectMapper = objectMapper;
		this.countriesUrl = countriesUrl;
	}

	public List<Country> fetchCountries() {
		String json = restClient.get()
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
