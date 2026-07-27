package com.cooperative.application.usecases;

import com.cooperative.application.dto.AssemblyResponse;
import com.cooperative.domain.entity.Assembly;
import com.cooperative.domain.ports.AssemblyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAssembliesUseCase Unit Tests")
class ListAssembliesUseCaseTest {

    @Mock
    private AssemblyRepository assemblyRepository;

    private ListAssembliesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListAssembliesUseCase(assemblyRepository);
    }

    @Test
    @DisplayName("Should list all assemblies")
    void testListAssembliesSuccess() {
        // Arrange
        Assembly assembly1 = Assembly.create("Meeting 1", "Description 1", Instant.now());
        Assembly assembly2 = Assembly.create("Meeting 2", "Description 2", null);

        when(assemblyRepository.findAll())
            .thenReturn(Arrays.asList(assembly1, assembly2));

        // Act
        List<AssemblyResponse> responses = useCase.execute();

        // Assert
        assertEquals(2, responses.size());
        assertEquals("Meeting 1", responses.get(0).title());
        assertEquals("Meeting 2", responses.get(1).title());
        verify(assemblyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no assemblies exist")
    void testListAssembliesEmpty() {
        // Arrange
        when(assemblyRepository.findAll())
            .thenReturn(Collections.emptyList());

        // Act
        List<AssemblyResponse> responses = useCase.execute();

        // Assert
        assertTrue(responses.isEmpty());
        verify(assemblyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return single assembly in list")
    void testListAssembliesSingle() {
        // Arrange
        Assembly assembly = Assembly.create("Only Meeting", null, null);

        when(assemblyRepository.findAll())
            .thenReturn(Collections.singletonList(assembly));

        // Act
        List<AssemblyResponse> responses = useCase.execute();

        // Assert
        assertEquals(1, responses.size());
        assertEquals("Only Meeting", responses.get(0).title());
    }
}
