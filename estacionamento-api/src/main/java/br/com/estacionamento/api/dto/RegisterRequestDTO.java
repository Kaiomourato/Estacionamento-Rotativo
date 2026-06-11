package br.com.estacionamento.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequestDTO {

    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "Informe um email válido.")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres.")
    private String senha;

    private String role;
    private Long estacionamentoId; // Será nulo para motoristas, mas obrigatório para operadores

    // Getters e Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getEstacionamentoId() { return estacionamentoId; }
    public void setEstacionamentoId(Long estacionamentoId) { this.estacionamentoId = estacionamentoId; }
}