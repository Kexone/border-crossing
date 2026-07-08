package com.prorocketeers.jakub.border_crossing.service;

import com.prorocketeers.jakub.border_crossing.client.CountryDataClient;
import com.prorocketeers.jakub.border_crossing.dto.Country;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoutingService {

	private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

	private final CountryDataClient countryDataClient;

	/**
	 * Adjacency map: cca3 code -> cca3 codes of countries sharing a land border.
	 * Rebuilt periodically by {@link #refreshBorderGraph()}; volatile so request
	 * threads always see the latest complete graph.
	 */
	private volatile Map<String, Set<String>> borderGraph;

	public RoutingService(CountryDataClient countryDataClient) {
		this.countryDataClient = countryDataClient;
	}

	@PostConstruct
	void buildBorderGraph() {
		borderGraph = countryDataClient.fetchCountries().stream()
				.collect(Collectors.toUnmodifiableMap(
						Country::cca3,
						country -> Set.copyOf(country.borders())));
		log.info("Border graph built with {} countries", borderGraph.size());
	}

	@Scheduled(initialDelayString = "${countries.cache.ttl}", fixedRateString = "${countries.cache.ttl}")
	void refreshBorderGraph() {
		try {
			buildBorderGraph();
		} catch (Exception e) {
			log.warn("Country data refresh failed, keeping cached data", e);
		}
	}

	/**
	 * Finds the shortest land route between two countries identified by their
	 * cca3 codes, using bidirectional BFS: two searches run from origin and
	 * destination and stop where their frontiers meet, exploring roughly
	 * O(b^(d/2)) nodes instead of O(b^d) for a plain BFS.
	 *
	 * @return the route including origin and destination, or empty if either
	 *         country is unknown or no land route exists
	 */
	public Optional<List<String>> findRoute(String origin, String destination) {
		Map<String, Set<String>> graph = borderGraph;
		if (!graph.containsKey(origin) || !graph.containsKey(destination)) {
			return Optional.empty();
		}
		if (origin.equals(destination)) {
			return Optional.of(List.of(origin));
		}

		// Parent maps double as visited sets; origin/destination map to null
		// as search roots.
		Map<String, String> parentsFromOrigin = new HashMap<>();
		Map<String, String> parentsFromDestination = new HashMap<>();
		parentsFromOrigin.put(origin, null);
		parentsFromDestination.put(destination, null);
		Deque<String> originFrontier = new ArrayDeque<>(List.of(origin));
		Deque<String> destinationFrontier = new ArrayDeque<>(List.of(destination));

		while (!originFrontier.isEmpty() && !destinationFrontier.isEmpty()) {
			// Always advance the smaller frontier to keep both searches balanced.
			String meetingPoint = originFrontier.size() <= destinationFrontier.size()
					? expandLevel(graph, originFrontier, parentsFromOrigin, parentsFromDestination)
					: expandLevel(graph, destinationFrontier, parentsFromDestination, parentsFromOrigin);
			if (meetingPoint != null) {
				return Optional.of(buildRoute(meetingPoint, parentsFromOrigin, parentsFromDestination));
			}
		}
		return Optional.empty();
	}

	/**
	 * Expands one full BFS level of the given frontier.
	 *
	 * @return the first node also visited by the opposite search, or null if
	 *         the frontiers have not met yet
	 */
	private String expandLevel(Map<String, Set<String>> graph, Deque<String> frontier,
			Map<String, String> parentsThisSide, Map<String, String> parentsOtherSide) {
		for (int nodesInLevel = frontier.size(); nodesInLevel > 0; nodesInLevel--) {
			String current = frontier.poll();
			for (String neighbor : graph.getOrDefault(current, Set.of())) {
				if (parentsThisSide.containsKey(neighbor)) {
					continue;
				}
				parentsThisSide.put(neighbor, current);
				if (parentsOtherSide.containsKey(neighbor)) {
					return neighbor;
				}
				frontier.add(neighbor);
			}
		}
		return null;
	}

	/**
	 * Reconstructs the full route by walking parent pointers from the meeting
	 * point back to origin and forward to destination.
	 */
	private List<String> buildRoute(String meetingPoint, Map<String, String> parentsFromOrigin,
			Map<String, String> parentsFromDestination) {
		List<String> route = new LinkedList<>();
		for (String node = meetingPoint; node != null; node = parentsFromOrigin.get(node)) {
			route.add(0, node);
		}
		for (String node = parentsFromDestination.get(meetingPoint); node != null;
				node = parentsFromDestination.get(node)) {
			route.add(node);
		}
		return List.copyOf(route);
	}
}
