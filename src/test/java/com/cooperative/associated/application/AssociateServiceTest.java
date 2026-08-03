package com.cooperative.associated.application;

import com.cooperative.associated.application.exception.AssociateNotFoundException;
import com.cooperative.associated.application.exception.DuplicateDocumentException;
import com.cooperative.associated.application.port.out.AssociateRepositoryPort;
import com.cooperative.associated.domain.Associate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssociateServiceTest {

    @Mock
    private AssociateRepositoryPort repository;

    private AssociateService service;

    private static final String VALID_NAME = "John Doe";
    private static final String VALID_DOCUMENT = "12345678900";
    private static final String VALID_EMAIL = "john.doe@example.com";

    @BeforeEach
    void setUp() {
        service = new AssociateService(repository);
    }

    @Test
    void shouldCreateAssociateWhenDocumentIsNotDuplicate() {
        when(repository.existsByDocument(VALID_DOCUMENT)).thenReturn(false);
        when(repository.save(any(Associate.class))).thenAnswer(inv -> inv.getArgument(0));

        Associate result = service.create(VALID_NAME, VALID_DOCUMENT, VALID_EMAIL);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(VALID_NAME);
        assertThat(result.getDocument()).isEqualTo(VALID_DOCUMENT);
        assertThat(result.getEmail()).isEqualTo(VALID_EMAIL);
        verify(repository).save(any(Associate.class));
    }

    @Test
    void shouldThrowWhenCreatingWithDuplicateDocument() {
        when(repository.existsByDocument(VALID_DOCUMENT)).thenReturn(true);

        assertThatThrownBy(() -> service.create(VALID_NAME, VALID_DOCUMENT, VALID_EMAIL))
                .isInstanceOf(DuplicateDocumentException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFindAssociateById() {
        UUID id = UUID.randomUUID();
        Associate associate = Associate.create(VALID_NAME, VALID_DOCUMENT, VALID_EMAIL);
        when(repository.findById(id)).thenReturn(Optional.of(associate));

        Associate result = service.findById(id);

        assertThat(result).isEqualTo(associate);
    }

    @Test
    void shouldThrowWhenAssociateNotFoundById() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(AssociateNotFoundException.class);
    }

    @Test
    void shouldReturnAllAssociates() {
        Associate a1 = Associate.create("Alice", "11111111111", null);
        Associate a2 = Associate.create("Bob", "22222222222", null);
        when(repository.findAll()).thenReturn(List.of(a1, a2));

        List<Associate> result = service.findAll();

        assertThat(result).containsExactly(a1, a2);
    }

    @Test
    void shouldUpdateAssociateWhenDataIsValid() {
        UUID id = UUID.randomUUID();
        Associate existing = Associate.create(VALID_NAME, VALID_DOCUMENT, VALID_EMAIL);
        lenient().when(repository.existsById(id)).thenReturn(true);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        lenient().when(repository.existsByDocumentAndIdNot(anyString(), eq(id))).thenReturn(false);
        when(repository.save(any(Associate.class))).thenAnswer(inv -> inv.getArgument(0));

        Associate result = service.update(id, "New Name", "99999999999", "new@example.com");

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getDocument()).isEqualTo("99999999999");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(repository).save(any(Associate.class));
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentAssociate() {
        UUID id = UUID.randomUUID();
        lenient().when(repository.existsById(id)).thenReturn(false);
        lenient().when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, VALID_NAME, VALID_DOCUMENT, VALID_EMAIL))
                .isInstanceOf(AssociateNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingToDuplicateDocumentOfAnotherAssociate() {
        UUID id = UUID.randomUUID();
        Associate existing = Associate.create(VALID_NAME, VALID_DOCUMENT, VALID_EMAIL);
        lenient().when(repository.existsById(id)).thenReturn(true);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.existsByDocumentAndIdNot(eq("99999999999"), eq(id))).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, VALID_NAME, "99999999999", VALID_EMAIL))
                .isInstanceOf(DuplicateDocumentException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldDeleteExistingAssociate() {
        UUID id = UUID.randomUUID();
        lenient().when(repository.existsById(id)).thenReturn(true);
        Associate existing = Associate.create(VALID_NAME, VALID_DOCUMENT, VALID_EMAIL);
        lenient().when(repository.findById(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentAssociate() {
        UUID id = UUID.randomUUID();
        lenient().when(repository.existsById(id)).thenReturn(false);
        lenient().when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(AssociateNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }
}

