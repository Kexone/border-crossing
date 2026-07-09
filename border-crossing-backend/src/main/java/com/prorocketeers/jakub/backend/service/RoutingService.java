package com.prorocketeers.jakub.backend.service;

import java.util.List;

public interface RoutingService {

	/**
	 * Finds the shortest land route between two countries identified by their
	 * cca3 codes.
	 *
	 * @return the route including origin and destination, or an empty list if
	 *         either country is unknown or no land route exists
	 */
	List<String> findRoute(String origin, String destination);
}
