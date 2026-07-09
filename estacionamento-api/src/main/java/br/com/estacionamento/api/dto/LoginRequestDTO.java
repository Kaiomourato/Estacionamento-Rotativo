package br.com.estacionamento.api.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequestDTO {

    // @Valid já estava no controller, mas sem estas anotações ele não validava nada:
    // e-mail/senha em branco chegavam até o AuthService e só falhavam depois, com uma
    // mensagem genérica (ou risco de NPE dentro do PasswordEncoder).
    @NotBlank(message = "O e-mail é obrigatório.")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    private String senha;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
