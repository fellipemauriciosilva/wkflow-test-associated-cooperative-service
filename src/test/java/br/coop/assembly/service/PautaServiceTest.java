package br.coop.assembly.service;

import br.coop.assembly.entity.Pauta;
import br.coop.assembly.exception.PautaNotFoundException;
import br.coop.assembly.exception.PautaValidationException;
import br.coop.assembly.repository.PautaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PautaService Tests")
class PautaServiceTest {
    @Mock
    private PautaRepository repository;

    @InjectMocks
    private PautaService service;

    private Pauta testPauta;

    @BeforeEach
    void setUp() {
        testPauta = new Pauta();
        testPauta.setId(1L);
        testPauta.setTitle("Test Pauta");
        testPauta.setDescription("Test Description");
        testPauta.setCreatedAt(LocalDateTime.now());
        testPauta.setUpdatedAt(LocalDateTime.now());
        testPauta.setIsDeleted(false);
    }

    @Test
    @DisplayName("Should create pauta with valid input")
    void createPauta_ValidInput_ReturnsSavedPauta() {
        String title = "Valid Pauta";
        String description = "Valid Description";

        Pauta savedPauta = new Pauta();
        savedPauta.setId(1L);
        savedPauta.setTitle(title);
        savedPauta.setDescription(description);
        savedPauta.setCreatedAt(LocalDateTime.now());
        savedPauta.setUpdatedAt(LocalDateTime.now());
        savedPauta.setIsDeleted(false);

        when(repository.save(any(Pauta.class))).thenReturn(savedPauta);

        Pauta result = service.createPauta(title, description);

        assertNotNull(result);
        assertEquals(savedPauta.getId(), result.getId());
        assertEquals(title, result.getTitle());
        assertEquals(description, result.getDescription());
        verify(repository, times(1)).save(any(Pauta.class));
    }

    @Test
    @DisplayName("Should throw exception when title is blank")
    void createPauta_BlankTitle_ThrowsException() {
        assertThrows(PautaValidationException.class, () ->
                service.createPauta("", "description")
        );
        verify(repository, never()).save(any(Pauta.class));
    }

    @Test
    @DisplayName("Should throw exception when title is null")
    void createPauta_NullTitle_ThrowsException() {
        assertThrows(PautaValidationException.class, () ->
                service.createPauta(null, "description")
        );
        verify(repository, never()).save(any(Pauta.class));
    }

    @Test
    @DisplayName("Should throw exception when title is too short")
    void createPauta_TitleTooShort_ThrowsException() {
        assertThrows(PautaValidationException.class, () ->
                service.createPauta("ab", "description")
        );
        verify(repository, never()).save(any(Pauta.class));
    }

    @Test
    @DisplayName("Should throw exception when title is too long")
    void createPauta_TitleTooLong_ThrowsException() {
        String longTitle = "a".repeat(256);
        assertThrows(PautaValidationException.class, () ->
                service.createPauta(longTitle, "description")
        );
        verify(repository, never()).save(any(Pauta.class));
    }

    @Test
    @DisplayName("Should throw exception when description exceeds max length")
    void createPauta_DescriptionTooLong_ThrowsException() {
        String longDescription = "a".repeat(1001);
        assertThrows(PautaValidationException.class, () ->
                service.createPauta("Valid Title", longDescription)
        );
        verify(repository, never()).save(any(Pauta.class));
    }

    @Test
    @DisplayName("Should allow null description")
    void createPauta_NullDescription_Success() {
        when(repository.save(any(Pauta.class))).thenReturn(testPauta);

        Pauta result = service.createPauta("Valid Title", null);

        assertNotNull(result);
        verify(repository, times(1)).save(any(Pauta.class));
    }

    @Test
    @DisplayName("Should retrieve pauta by valid id")
    void getPautaById_ValidId_ReturnsPauta() {
        when(repository.findById(1L)).thenReturn(Optional.of(testPauta));

        Pauta result = service.getPautaById(1L);

        assertNotNull(result);
        assertEquals(testPauta.getId(), result.getId());
        assertEquals(testPauta.getTitle(), result.getTitle());
    }

    @Test
    @DisplayName("Should throw exception when pauta not found")
    void getPautaById_InvalidId_ThrowsException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PautaNotFoundException.class, () ->
                service.getPautaById(999L)
        );
    }

    @Test
    @DisplayName("Should return all pautas")
    void getAllPautas_ReturnsListFromRepository() {
        Pauta pauta2 = new Pauta();
        pauta2.setId(2L);
        pauta2.setTitle("Second Pauta");
        List<Pauta> pautas = Arrays.asList(testPauta, pauta2);

        when(repository.findAll()).thenReturn(pautas);

        List<Pauta> result = service.getAllPautas();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testPauta.getId(), result.get(0).getId());
        assertEquals(pauta2.getId(), result.get(1).getId());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should accept minimum valid title length (3 chars)")
    void createPauta_MinimumTitleLength_Success() {
        String title = "abc";
        when(repository.save(any(Pauta.class))).thenReturn(testPauta);

        Pauta result = service.createPauta(title, "description");

        assertNotNull(result);
        verify(repository, times(1)).save(any(Pauta.class));
    }

    @Test
    @DisplayName("Should accept maximum valid title length (255 chars)")
    void createPauta_MaximumTitleLength_Success() {
        String title = "a".repeat(255);
        when(repository.save(any(Pauta.class))).thenReturn(testPauta);

        Pauta result = service.createPauta(title, "description");

        assertNotNull(result);
        verify(repository, times(1)).save(any(Pauta.class));
    }

    @Test
    @DisplayName("Should accept maximum valid description length (1000 chars)")
    void createPauta_MaximumDescriptionLength_Success() {
        String description = "a".repeat(1000);
        when(repository.save(any(Pauta.class))).thenReturn(testPauta);

        Pauta result = service.createPauta("Valid Title", description);

        assertNotNull(result);
        verify(repository, times(1)).save(any(Pauta.class));
    }
}
