package com.prorocketeers.jakub.app.controller;

import com.prorocketeers.jakub.app.api.RoutingApi;
import com.prorocketeers.jakub.app.api.model.RouteResponse;
import com.prorocketeers.jakub.app.exception.NoLandRouteException;
import com.prorocketeers.jakub.backend.service.RoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
public class RoutingController implements RoutingApi {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @Override
    public ResponseEntity<RouteResponse> route(String origin, String destination) {
        var from = origin.toUpperCase(Locale.ROOT);
        var to = destination.toUpperCase(Locale.ROOT);
        var route = routingService.findRoute(from, to);
        if (route.isEmpty()) {
            throw new NoLandRouteException(from, to);
        }
        return ResponseEntity.ok(new RouteResponse(route));
    }
}
