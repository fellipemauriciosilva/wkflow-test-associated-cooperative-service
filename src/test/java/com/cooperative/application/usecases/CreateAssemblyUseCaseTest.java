package com.cooperative.application.usecases;

import com.cooperative.application.dto.AssemblyResponse;
import com.cooperative.application.dto.CreateAssemblyRequest;
import com.cooperative.domain.entity.Assembly;
import com.cooperative.domain.exception.InvalidAssemblyException;
import com.cooperative.domain.ports.AssemblyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAssemblyUseCase Unit Tests")
class CreateAssemblyUseCaseTest {

    @Mock
    private AssemblyRepository assemblyRepository;

    private CreateAssemblyUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateAssemblyUseCase(assemblyRepository);
    }

    // ========== SUCCESS SCENARIOS ==========

    @Test
    @DisplayName("Should create Assembly with title only")
    void testCreateAssemblyWithTitleOnly() {
        // Arrange
        CreateAssemblyRequest request = new CreateAssemblyRequest(
            "Board Meeting",
            null,
            null
        );

        Assembly savedAssembly = Assembly.create(
            request.title(),
            request.description(),
            request.scheduledAt()
        );

        when(assemblyRepository.save(any(Assembly.class)))
            .thenReturn(savedAssembly);

        // Act
        AssemblyResponse response = useCase.execute(request);

        // Assert
        assertNotNull(response);
        assertEquals("Board Meeting", response.title());
        assertNull(response.description());
        assertNull(response.scheduledAt());
        verify(assemblyRepository, times(1)).save(any(Assembly.class));
    }

    @Test
    @DisplayName("Should create Assembly with all fields")
    void testCreateAssemblyWithAllFields() {
        // Arrange
        Instant scheduledAt = Instant.parse("2025-03-15T14:00:00Z");
        CreateAssemblyRequest request = new CreateAssemblyRequest(
            "Quarterly Review",
            "Discuss Q1 performance",
            scheduledAt
        );

        Assembly savedAssembly = Assembly.create(
            request.title(),
            request.description(),
            request.scheduledAt()
        );

        when(assemblyRepository.save(any(Assembly.class)))
            .thenReturn(savedAssembly);

        // Act
        AssemblyResponse response = useCase.execute(request);

        // Assert
        assertEquals("Quarterly Review", response.title());
        assertEquals("Discuss Q1 performance", response.description());
        assertEquals(scheduledAt, response.scheduledAt());
    }

    @Test
    @DisplayName("Should create Assembly with title and description (no scheduledAt)")
    void testCreateAssemblyWithoutScheduledAt() {
        // Arrange
        CreateAssemblyRequest request = new CreateAssemblyRequest(
            "Annual Meeting",
            "Year-end review",
            null
        );

        Assembly savedAssembly = Assembly.create(
            request.title(),
            request.description(),
            request.scheduledAt()
        );

        when(assemblyRepository.save(any(Assembly.class)))
            .thenReturn(savedAssembly);

        // Act
        AssemblyResponse response = useCase.execute(request);

        // Assert
        assertEquals("Annual Meeting", response.title());
        assertEquals("Year-end review", response.description());
        assertNull(response.scheduledAt());
    }

    // ========== VALIDATION FAILURE SCENARIOS ==========

    @Test
    @DisplayName("Should throw InvalidAssemblyException when title is null")
    void testCreateAssemblyWithNullTitle() {
        // Arrange
        CreateAssemblyRequest request = new CreateAssemblyRequest(
            null,
            "Some description",
            null
        );

        // Act & Assert
        assertThrows(InvalidAssemblyException.class, () ->
            useCase.execute(request)
        );
        verify(assemblyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidAssemblyException when title is empty")
    void testCreateAssemblyWithEmptyTitle() {
        // Arrange
        CreateAssemblyRequest request = new CreateAssemblyRequest(
            "",
            "Some description",
            null
        );

        // Act & Assert
        assertThrows(InvalidAssemblyException.class, () ->
            useCase.execute(request)
        );
        verify(assemblyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidAssemblyException when title is whitespace")
    void testCreateAssemblyWithWhitespaceTitle() {
        // Arrange
        CreateAssemblyRequest request = new CreateAssemblyRequest(
            "   ",
            "Some description",
            null
        );

        // Act & Assert
        assertThrows(InvalidAssemblyException.class, () ->
            useCase.execute(request)
        );
        verify(assemblyRepository, never()).save(any());
    }

    // ========== REPOSITORY INTERACTION TESTS ==========

    @Test
    @DisplayName("Should pass correct Assembly entity to repository.save()")
    void testSaveCalledWithCorrectEntity() {
        // Arrange
        Instant scheduledAt = Instant.parse("2025-06-30T10:00:00Z");
        CreateAssemblyRequest request = new CreateAssemblyRequest(
            "Annual Meeting",
            "Year review",
            scheduledAt
        );

        Assembly savedAssembly = Assembly.create(
            request.title(),
            request.description(),
            request.scheduledAt()
        );

        when(assemblyRepository.save(any(Assembly.class)))
            .thenReturn(savedAssembly);

        // Act
        useCase.execute(request);

        // Assert
        verify(assemblyRepository).save(argThat(assembly ->
            assembly.title().equals("Annual Meeting") &&
            assembly.description().equals("Year review") &&
            assembly.scheduledAt().equals(scheduledAt)
        ));
    }

    @Test
    @DisplayName("Should return response with saved assembly ID")
    void testResponseIncludesPersistedId() {
        // Arrange
        CreateAssemblyRequest request = new CreateAssemblyRequest(
            "Meeting",
            null,
            null
        );

        Assembly savedAssembly = Assembly.create(
            request.title(),
            request.description(),
            request.scheduledAt()
        );

        when(assemblyRepository.save(any(Assembly.class)))
            .thenReturn(savedAssembly);

        // Act
        AssemblyResponse response = useCase.execute(request);

        // Assert
        assertNotNull(response);
        assertEquals("Meeting", response.title());
    }
}
