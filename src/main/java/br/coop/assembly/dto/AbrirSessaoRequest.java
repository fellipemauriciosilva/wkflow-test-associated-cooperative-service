package br.coop.assembly.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbrirSessaoRequest {
    private Integer duracaoMinutos;
}
