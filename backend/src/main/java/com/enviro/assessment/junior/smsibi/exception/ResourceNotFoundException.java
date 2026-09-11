package com.enviro.assessment.junior.smsibi.exception;

/**
 * Thrown when a requested investor, product or withdrawal does not exist. Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super(resourceName + " with id " + id + " was not found.");
    }
}
