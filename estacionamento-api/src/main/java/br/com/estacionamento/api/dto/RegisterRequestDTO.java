package br.com.estacionamento.api.dto;

public class RegisterRequestDTO {

    private String email;
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