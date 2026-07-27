package com.cooperative.application.usecases;

import com.cooperative.application.dto.AssociateResponse;
import com.cooperative.domain.entity.Associate;
import com.cooperative.domain.ports.AssociateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAssociatesUseCase Unit Tests")
class ListAssociatesUseCaseTest {

    @Mock
    private AssociateRepository associateRepository;

    private ListAssociatesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListAssociatesUseCase(associateRepository);
    }

    @Test
    @DisplayName("Should list all associates")
    void testListAssociatesSuccess() {
        // Arrange
        Associate associate1 = Associate.create("John Doe", "12345678901", "john@example.com");
        Associate associate2 = Associate.create("Jane Smith", "98765432100", "jane@example.com");

        when(associateRepository.findAll())
            .thenReturn(Arrays.asList(associate1, associate2));

        // Act
        List<AssociateResponse> responses = useCase.execute();

        // Assert
        assertEquals(2, responses.size());
        assertEquals("John Doe", responses.get(0).name());
        assertEquals("Jane Smith", responses.get(1).name());
        verify(associateRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no associates exist")
    void testListAssociatesEmpty() {
        // Arrange
        when(associateRepository.findAll())
            .thenReturn(Collections.emptyList());

        // Act
        List<AssociateResponse> responses = useCase.execute();

        // Assert
        assertTrue(responses.isEmpty());
        verify(associateRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return single associate in list")
    void testListAssociatesSingle() {
        // Arrange
        Associate associate = Associate.create("Bob Jones", "11111111111", null);

        when(associateRepository.findAll())
            .thenReturn(Collections.singletonList(associate));

        // Act
        List<AssociateResponse> responses = useCase.execute();

        // Assert
        assertEquals(1, responses.size());
        assertEquals("Bob Jones", responses.get(0).name());
    }

    @Test
    @DisplayName("Should handle associates without email")
    void testListAssociatesWithoutEmail() {
        // Arrange
        Associate associate1 = Associate.create("Alice", "11111111111", null);
        Associate associate2 = Associate.create("Bob", "22222222222", "bob@example.com");

        when(associateRepository.findAll())
            .thenReturn(Arrays.asList(associate1, associate2));

        // Act
        List<AssociateResponse> responses = useCase.execute();

        // Assert
        assertEquals(2, responses.size());
        assertNull(responses.get(0).email());
        assertNotNull(responses.get(1).email());
    }
}
