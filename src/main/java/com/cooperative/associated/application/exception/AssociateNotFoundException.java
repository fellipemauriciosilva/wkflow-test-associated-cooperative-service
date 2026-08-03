package com.cooperative.associated.application.exception;

import java.util.UUID;

public class AssociateNotFoundException extends RuntimeException {

    public AssociateNotFoundException(UUID id) {
        super("associate not found: " + id);
    }
}



