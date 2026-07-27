package com.cooperative.application.usecases;

import com.cooperative.application.dto.AssociateResponse;
import com.cooperative.application.dto.CreateAssociateRequest;
import com.cooperative.domain.entity.Associate;
import com.cooperative.domain.exception.DuplicateDocumentException;
import com.cooperative.domain.exception.InvalidAssociateException;
import com.cooperative.domain.ports.AssociateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAssociateUseCaseTest {

    @Mock
    private AssociateRepository repository;

    private CreateAssociateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateAssociateUseCase(repository);
    }

    @Test
    void testExecuteWithValidRequest() {
        CreateAssociateRequest request = new CreateAssociateRequest(
            "John Doe", "12345678901", "john@example.com");

        Associate saved = new Associate(
            1L, "John Doe", "12345678901", "john@example.com", Instant.now());
        when(repository.findByDocument("12345678901")).thenReturn(Optional.empty());
        when(repository.save(any(Associate.class))).thenReturn(saved);

        AssociateResponse response = useCase.execute(request);

        assertEquals(1L, response.id());
        assertEquals("John Doe", response.name());
        verify(repository).findByDocument("12345678901");
        verify(repository).save(any(Associate.class));
    }

    @Test
    void testExecuteWithDuplicateDocument() {
        CreateAssociateRequest request = new CreateAssociateRequest(
            "John Doe", "12345678901", "john@example.com");

        Associate existing = new Associate(
            1L, "Jane Doe", "12345678901", "jane@example.com", Instant.now());
        when(repository.findByDocument("12345678901")).thenReturn(Optional.of(existing));

        DuplicateDocumentException ex = assertThrows(
            DuplicateDocumentException.class,
            () -> useCase.execute(request)
        );
        assertTrue(ex.getMessage().contains("already exists"));
        verify(repository, never()).save(any());
    }

    @Test
    void testExecuteWithInvalidInput() {
        CreateAssociateRequest request = new CreateAssociateRequest(
            "J", "12345678901", "invalid-email");

        InvalidAssociateException ex = assertThrows(
            InvalidAssociateException.class,
            () -> useCase.execute(request)
        );
        assertNotNull(ex.getMessage());
        verify(repository, never()).save(any());
    }
}
