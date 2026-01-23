package br.com.estacionamento.api.dto;

public class LoginResponseDTO {

    private Long id;
    private String nome;
    private String email;

    public LoginResponseDTO(Long id, String nome, String email) {
        this.id = id;
        this.nome = nome;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }
}
