package com.prorocketeers.jakub.backend.service;

import com.prorocketeers.jakub.country.connector.CountryDataClient;
import com.prorocketeers.jakub.country.connector.dto.Country;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
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
     * Finds the shortest land route using bidirectional BFS: one search front
     * grows from the origin, another from the destination, and the route is
     * complete where they meet. Exploring two half-depth trees visits roughly
     * O(b^(d/2)) nodes instead of O(b^d) for a single-direction BFS.
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

        var forward = new SearchFront(origin);
        var backward = new SearchFront(destination);
        while (forward.hasNodesToExpand() && backward.hasNodesToExpand()) {
            // Always advance the smaller front; this keeps both searches at
            // similar depth, which is what makes the meeting point optimal work.
            var smaller = forward.size() <= backward.size() ? forward : backward;
            var opposite = smaller == forward ? backward : forward;

            var meetingPoint = smaller.expandNextLevel(graph, opposite);
            if (meetingPoint != null) {
                return joinAtMeetingPoint(meetingPoint, forward, backward);
            }
        }
        return List.of();
    }

    /**
     * Builds the full origin-to-destination route by following parent links
     * from the meeting point out to both roots.
     */
    private static List<String> joinAtMeetingPoint(String meetingPoint,
            SearchFront forward, SearchFront backward) {
        var route = new ArrayList<String>();
        for (var node = meetingPoint; node != null; node = forward.parentOf(node)) {
            route.add(node);
        }
        Collections.reverse(route); // walked meeting point -> origin, route reads origin -> meeting point
        for (var node = backward.parentOf(meetingPoint); node != null; node = backward.parentOf(node)) {
            route.add(node);
        }
        return List.copyOf(route);
    }

    /**
     * One direction of the bidirectional search: a BFS frontier plus parent
     * links for route reconstruction. The parent map doubles as the visited
     * set; the root is marked visited by mapping to null.
     */
    private static final class SearchFront {

        private final Deque<String> frontier = new ArrayDeque<>();
        private final Map<String, String> parents = new HashMap<>();

        SearchFront(String root) {
            frontier.add(root);
            parents.put(root, null);
        }

        /**
         * Expands the frontier by exactly one BFS level.
         *
         * @return the first node also visited by the opposite front, or null
         *         if the fronts have not met yet
         */
        String expandNextLevel(Map<String, Set<String>> graph, SearchFront opposite) {
            for (var remaining = frontier.size(); remaining > 0; remaining--) {
                var current = frontier.poll();
                for (var neighbor : graph.getOrDefault(current, Set.of())) {
                    if (parents.containsKey(neighbor)) {
                        continue; // already visited from this side
                    }
                    parents.put(neighbor, current);
                    if (opposite.hasVisited(neighbor)) {
                        return neighbor;
                    }
                    frontier.add(neighbor);
                }
            }
            return null;
        }

        boolean hasNodesToExpand() {
            return !frontier.isEmpty();
        }

        int size() {
            return frontier.size();
        }

        boolean hasVisited(String node) {
            return parents.containsKey(node);
        }

        String parentOf(String node) {
            return parents.get(node);
        }
    }
}
