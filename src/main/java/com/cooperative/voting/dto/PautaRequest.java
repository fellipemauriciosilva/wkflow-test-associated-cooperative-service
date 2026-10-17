package com.cooperative.voting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request payload for creating a new {@code Pauta}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PautaRequest {

    @NotBlank(message = "must not be blank")
    private String title;

    @NotBlank(message = "must not be blank")
    private String description;

    @NotNull(message = "must not be null")
    private LocalDateTime date;
}
