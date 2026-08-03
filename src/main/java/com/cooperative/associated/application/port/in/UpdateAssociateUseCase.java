package com.cooperative.associated.application.port.in;

import com.cooperative.associated.domain.Associate;

import java.util.UUID;

/**
 * Input port: updates an existing Associate.
 */
public interface UpdateAssociateUseCase {

    Associate update(UUID id, String name, String document, String email);
}


