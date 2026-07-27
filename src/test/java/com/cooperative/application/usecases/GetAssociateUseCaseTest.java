package com.cooperative.application.usecases;

import com.cooperative.application.dto.AssociateResponse;
import com.cooperative.domain.entity.Associate;
import com.cooperative.domain.exception.EntityNotFoundException;
import com.cooperative.domain.ports.AssociateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAssociateUseCase Unit Tests")
class GetAssociateUseCaseTest {

    @Mock
    private AssociateRepository associateRepository;

    private GetAssociateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAssociateUseCase(associateRepository);
    }

    @Test
    @DisplayName("Should retrieve existing associate by ID")
    void testGetAssociateSuccess() {
        // Arrange
        Long associateId = 1L;
        Associate associate = Associate.create("John Doe", "12345678901", "john@example.com");

        when(associateRepository.findById(associateId))
            .thenReturn(Optional.of(associate));

        // Act
        AssociateResponse response = useCase.execute(associateId);

        // Assert
        assertNotNull(response);
        assertEquals("John Doe", response.name());
        assertEquals("12345678901", response.document());
        assertEquals("john@example.com", response.email());
        verify(associateRepository, times(1)).findById(associateId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when associate not found")
    void testGetAssociateNotFound() {
        // Arrange
        Long associateId = 99999L;

        when(associateRepository.findById(associateId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
            useCase.execute(associateId)
        );
        verify(associateRepository, times(1)).findById(associateId);
    }

    @Test
    @DisplayName("Should return DTO with all associate fields")
    void testResponseIncludesAllFields() {
        // Arrange
        Long associateId = 1L;
        Associate associate = Associate.create("Jane Smith", "98765432100", "jane@example.com");

        when(associateRepository.findById(associateId))
            .thenReturn(Optional.of(associate));

        // Act
        AssociateResponse response = useCase.execute(associateId);

        // Assert
        assertEquals("Jane Smith", response.name());
        assertEquals("98765432100", response.document());
        assertEquals("jane@example.com", response.email());
    }

    @Test
    @DisplayName("Should handle associate without email")
    void testGetAssociateWithoutEmail() {
        // Arrange
        Long associateId = 1L;
        Associate associate = Associate.create("Bob Jones", "11111111111", null);

        when(associateRepository.findById(associateId))
            .thenReturn(Optional.of(associate));

        // Act
        AssociateResponse response = useCase.execute(associateId);

        // Assert
        assertEquals("Bob Jones", response.name());
        assertNull(response.email());
    }
}
