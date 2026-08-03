package com.cooperative.associated.application.port.in;

import com.cooperative.associated.domain.Associate;

import java.util.List;

/**
 * Input port: lists all Associates.
 */
public interface ListAssociatesUseCase {

    List<Associate> findAll();
}


