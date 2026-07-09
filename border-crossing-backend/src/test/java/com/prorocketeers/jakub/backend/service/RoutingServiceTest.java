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
class RoutingServiceTest {

	@Mock
	private CountryDataClient countryDataClient;

	private RoutingServiceImpl routingService;

	/**
	 * Fixture map (borders are symmetric, as in the real data set):
	 *
	 * <pre>
	 * POL - CZE - SVK
	 *  |   /   \
	 * DEU --- AUT
	 *  |        \
	 * FRA ----- ITA
	 *  |
	 * ESP - PRT          AUS   NZL (islands)
	 * </pre>
	 */
	@BeforeEach
	void setUp() {
		when(countryDataClient.fetchCountries()).thenReturn(List.of(
				new Country("CZE", List.of("DEU", "AUT", "POL", "SVK")),
				new Country("DEU", List.of("CZE", "AUT", "FRA", "POL")),
				new Country("AUT", List.of("CZE", "DEU", "ITA")),
				new Country("ITA", List.of("AUT", "FRA")),
				new Country("FRA", List.of("DEU", "ITA", "ESP")),
				new Country("ESP", List.of("FRA", "PRT")),
				new Country("PRT", List.of("ESP")),
				new Country("POL", List.of("CZE", "DEU")),
				new Country("SVK", List.of("CZE")),
				new Country("AUS", List.of()),
				new Country("NZL", List.of())));

		routingService = new RoutingServiceImpl(countryDataClient);
		routingService.buildBorderGraph();
	}

	@Test
	void findsDirectRouteBetweenNeighbours() {
		assertThat(routingService.findRoute("CZE", "DEU"))
				.containsExactly("CZE", "DEU");
	}

	@Test
	void findsShortestRouteWithOneCrossing() {
		// CZE-AUT-ITA (3) beats CZE-DEU-AUT-ITA (4)
		assertThat(routingService.findRoute("CZE", "ITA"))
				.containsExactly("CZE", "AUT", "ITA");
	}

	@Test
	void findsShortestRouteAcrossSeveralCountries() {
		// CZE-DEU-FRA-ESP-PRT (5) beats CZE-AUT-ITA-FRA-ESP-PRT (6)
		assertThat(routingService.findRoute("CZE", "PRT"))
				.containsExactly("CZE", "DEU", "FRA", "ESP", "PRT");
	}

	@Test
	void findsReversedRouteForSwappedArguments() {
		assertThat(routingService.findRoute("PRT", "CZE"))
				.containsExactly("PRT", "ESP", "FRA", "DEU", "CZE");
	}

	@Test
	void returnsSingleCountryWhenOriginEqualsDestination() {
		assertThat(routingService.findRoute("CZE", "CZE"))
				.containsExactly("CZE");
	}

	@Test
	void returnsEmptyForUnknownOrigin() {
		assertThat(routingService.findRoute("XXX", "CZE")).isEmpty();
	}

	@Test
	void returnsEmptyForUnknownDestination() {
		assertThat(routingService.findRoute("CZE", "XXX")).isEmpty();
	}

	@Test
	void returnsEmptyWhenDestinationHasNoLandBorder() {
		assertThat(routingService.findRoute("CZE", "AUS")).isEmpty();
	}

	@Test
	void returnsEmptyWhenOriginHasNoLandBorder() {
		assertThat(routingService.findRoute("AUS", "CZE")).isEmpty();
	}

	@Test
	void returnsEmptyBetweenTwoIslands() {
		assertThat(routingService.findRoute("AUS", "NZL")).isEmpty();
	}

	@Test
	void everyConsecutivePairInRouteSharesBorder() {
		var route = routingService.findRoute("SVK", "PRT");

		assertThat(route).first().isEqualTo("SVK");
		assertThat(route).last().isEqualTo("PRT");
		List<Country> countries = countryDataClient.fetchCountries();
		for (int i = 0; i < route.size() - 1; i++) {
			var current = route.get(i);
			var next = route.get(i + 1);
			assertThat(countries)
					.filteredOn(country -> country.cca3().equals(current))
					.singleElement()
					.satisfies(country -> assertThat(country.borders()).contains(next));
		}
	}

	@Test
	void returnsEmptyOptionalNotNullForImpossibleRoutes() {
		assertThat(routingService.findRoute("CZE", "AUS")).isNotNull().isEmpty();
	}
}
