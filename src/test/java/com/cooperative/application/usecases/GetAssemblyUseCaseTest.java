package com.cooperative.application.usecases;

import com.cooperative.application.dto.AssemblyResponse;
import com.cooperative.domain.entity.Assembly;
import com.cooperative.domain.exception.EntityNotFoundException;
import com.cooperative.domain.ports.AssemblyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAssemblyUseCase Unit Tests")
class GetAssemblyUseCaseTest {

    @Mock
    private AssemblyRepository assemblyRepository;

    private GetAssemblyUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAssemblyUseCase(assemblyRepository);
    }

    @Test
    @DisplayName("Should retrieve existing assembly by ID")
    void testGetAssemblySuccess() {
        // Arrange
        Long assemblyId = 1L;
        Instant scheduledAt = Instant.parse("2025-03-15T14:00:00Z");
        Assembly assembly = Assembly.create("Board Meeting", "Quarterly", scheduledAt);

        when(assemblyRepository.findById(assemblyId))
            .thenReturn(Optional.of(assembly));

        // Act
        AssemblyResponse response = useCase.execute(assemblyId);

        // Assert
        assertNotNull(response);
        assertEquals("Board Meeting", response.title());
        assertEquals("Quarterly", response.description());
        verify(assemblyRepository, times(1)).findById(assemblyId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when assembly not found")
    void testGetAssemblyNotFound() {
        // Arrange
        Long assemblyId = 99999L;

        when(assemblyRepository.findById(assemblyId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
            useCase.execute(assemblyId)
        );
        verify(assemblyRepository, times(1)).findById(assemblyId);
    }

    @Test
    @DisplayName("Should return DTO with all assembly fields")
    void testResponseIncludesAllFields() {
        // Arrange
        Long assemblyId = 1L;
        Instant scheduledAt = Instant.parse("2025-12-31T23:59:59Z");
        Assembly assembly = Assembly.create("New Year", "Review year", scheduledAt);

        when(assemblyRepository.findById(assemblyId))
            .thenReturn(Optional.of(assembly));

        // Act
        AssemblyResponse response = useCase.execute(assemblyId);

        // Assert
        assertEquals("New Year", response.title());
        assertEquals("Review year", response.description());
        assertEquals(scheduledAt, response.scheduledAt());
    }
}
