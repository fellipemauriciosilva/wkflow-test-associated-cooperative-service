package com.cooperative.associated.application.port.in;

import java.util.UUID;

/**
 * Input port: deletes an Associate by id.
 */
public interface DeleteAssociateUseCase {

    void delete(UUID id);
}
