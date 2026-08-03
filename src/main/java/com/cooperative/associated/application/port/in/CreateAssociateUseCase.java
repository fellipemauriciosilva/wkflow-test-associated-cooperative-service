package com.cooperative.associated.application.port.in;

import com.cooperative.associated.domain.Associate;

/**
 * Input port: creates a new Associate.
 */
public interface CreateAssociateUseCase {

    Associate create(String name, String document, String email);
}
