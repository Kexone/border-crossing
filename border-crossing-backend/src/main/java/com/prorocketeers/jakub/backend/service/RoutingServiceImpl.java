package com.prorocketeers.jakub.backend.service;

import com.prorocketeers.jakub.country.connector.CountryDataClient;
import com.prorocketeers.jakub.country.connector.dto.Country;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoutingServiceImpl implements RoutingService {

	private static final Logger log = LoggerFactory.getLogger(RoutingServiceImpl.class);

	private final CountryDataClient countryDataClient;

	/**
	 * Adjacency map: cca3 code -> cca3 codes of countries sharing a land border.
	 * Rebuilt periodically by {@link BorderGraphRefreshScheduler}; volatile so
	 * request threads always see the latest complete graph.
	 */
	private volatile Map<String, Set<String>> borderGraph;

	public RoutingServiceImpl(CountryDataClient countryDataClient) {
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

	/**
	 * Finds the shortest land route using bidirectional BFS: two searches run
	 * from origin and destination and stop where their frontiers meet, exploring
	 * roughly O(b^(d/2)) nodes instead of O(b^d) for a plain BFS.
	 */
	@Override
	public List<String> findRoute(String origin, String destination) {
		var graph = borderGraph;
		if (!graph.containsKey(origin) || !graph.containsKey(destination)) {
			return List.of();
		}
		if (origin.equals(destination)) {
			return List.of(origin);
		}

		// Parent maps double as visited sets; origin/destination map to null
		// as search roots.
		var parentsFromOrigin = new HashMap<String, String>();
		var parentsFromDestination = new HashMap<String, String>();
		parentsFromOrigin.put(origin, null);
		parentsFromDestination.put(destination, null);
		var originFrontier = new ArrayDeque<>(List.of(origin));
		var destinationFrontier = new ArrayDeque<>(List.of(destination));

		while (!originFrontier.isEmpty() && !destinationFrontier.isEmpty()) {
			// Always advance the smaller frontier to keep both searches balanced.
			var meetingPoint = originFrontier.size() <= destinationFrontier.size()
					? expandLevel(graph, originFrontier, parentsFromOrigin, parentsFromDestination)
					: expandLevel(graph, destinationFrontier, parentsFromDestination, parentsFromOrigin);
			if (meetingPoint != null) {
				return buildRoute(meetingPoint, parentsFromOrigin, parentsFromDestination);
			}
		}
		return List.of();
	}

	/**
	 * Expands one full BFS level of the given frontier.
	 *
	 * @return the first node also visited by the opposite search, or null if
	 *         the frontiers have not met yet
	 */
	private String expandLevel(Map<String, Set<String>> graph, Deque<String> frontier,
			Map<String, String> parentsThisSide, Map<String, String> parentsOtherSide) {
		for (var nodesInLevel = frontier.size(); nodesInLevel > 0; nodesInLevel--) {
			var current = frontier.poll();
			for (var neighbor : graph.getOrDefault(current, Set.of())) {
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
		var route = new LinkedList<String>();
		for (var node = meetingPoint; node != null; node = parentsFromOrigin.get(node)) {
			route.add(0, node);
		}
		for (var node = parentsFromDestination.get(meetingPoint); node != null;
				node = parentsFromDestination.get(node)) {
			route.add(node);
		}
		return List.copyOf(route);
	}
}
