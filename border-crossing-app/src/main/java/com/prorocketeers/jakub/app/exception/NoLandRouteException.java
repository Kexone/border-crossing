package com.prorocketeers.jakub.app.exception;

public class NoLandRouteException extends RuntimeException {

    public NoLandRouteException(String origin, String destination) {
        super("No land route from %s to %s".formatted(origin, destination));
    }
}
