package com.prorocketeers.jakub.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class BorderGraphRefreshScheduler {

	private static final Logger log = LoggerFactory.getLogger(BorderGraphRefreshScheduler.class);

	private final RoutingServiceImpl routingService;

	BorderGraphRefreshScheduler(RoutingServiceImpl routingService) {
		this.routingService = routingService;
	}

	@Scheduled(initialDelayString = "${countries.cache.ttl}", fixedRateString = "${countries.cache.ttl}")
	void refreshBorderGraph() {
		try {
			routingService.buildBorderGraph();
		} catch (Exception e) {
			log.warn("Country data refresh failed, keeping cached data", e);
		}
	}
}
