package pautas;

import java.time.Instant;
import java.util.List;

public record ErroResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<CampoErro> fieldErrors) {

    public record CampoErro(String field, String message) {
    }
}
