package com.cooperative.associated.application.port.in;

import com.cooperative.associated.domain.Associate;

import java.util.UUID;

/**
 * Input port: retrieves an Associate by id.
 */
public interface GetAssociateUseCase {

    Associate findById(UUID id);
}
