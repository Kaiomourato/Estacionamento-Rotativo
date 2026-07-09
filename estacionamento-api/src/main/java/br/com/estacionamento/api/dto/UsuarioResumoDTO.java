package br.com.estacionamento.api.dto;

import java.time.LocalDateTime;

public class UsuarioResumoDTO {

    private Long id;
    private String email;
    private String role;
    // Papel funcional derivado: ADMIN, OPERADOR (role=USER com estacionamento vinculado) ou MOTORISTA
    private String tipo;
    private String estacionamentoNome;
    private LocalDateTime criadoEm;

    public UsuarioResumoDTO(Long id, String email, String role, String tipo, String estacionamentoNome, LocalDateTime criadoEm) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.tipo = tipo;
        this.estacionamentoNome = estacionamentoNome;
        this.criadoEm = criadoEm;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getTipo() { return tipo; }
    public String getEstacionamentoNome() { return estacionamentoNome; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
