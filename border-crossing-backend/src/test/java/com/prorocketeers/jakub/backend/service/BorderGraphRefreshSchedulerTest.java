package com.prorocketeers.jakub.backend.service;

import com.prorocketeers.jakub.country.connector.CountryDataClient;
import com.prorocketeers.jakub.country.connector.dto.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorderGraphRefreshSchedulerTest {

	@Mock
	private CountryDataClient countryDataClient;

	private RoutingServiceImpl routingService;
	private BorderGraphRefreshScheduler scheduler;

	@BeforeEach
	void setUp() {
		when(countryDataClient.fetchCountries()).thenReturn(List.of(
				new Country("CZE", List.of("DEU", "AUT")),
				new Country("DEU", List.of("CZE", "AUT")),
				new Country("AUT", List.of("CZE", "DEU", "ITA")),
				new Country("ITA", List.of("AUT"))));

		routingService = new RoutingServiceImpl(countryDataClient);
		routingService.buildBorderGraph();
		scheduler = new BorderGraphRefreshScheduler(routingService);
	}

	@Test
	void keepsServingCachedGraphWhenRefreshFails() {
		when(countryDataClient.fetchCountries())
				.thenThrow(new IllegalStateException("data source down"));

		scheduler.refreshBorderGraph();

		assertThat(routingService.findRoute("CZE", "ITA"))
				.containsExactly("CZE", "AUT", "ITA");
	}

	@Test
	void refreshRebuildsGraphFromFreshData() {
		when(countryDataClient.fetchCountries()).thenReturn(List.of(
				new Country("CZE", List.of("DEU")),
				new Country("DEU", List.of("CZE"))));

		scheduler.refreshBorderGraph();

		assertThat(routingService.findRoute("CZE", "DEU")).containsExactly("CZE", "DEU");
		assertThat(routingService.findRoute("CZE", "AUT")).isEmpty();
	}
}
