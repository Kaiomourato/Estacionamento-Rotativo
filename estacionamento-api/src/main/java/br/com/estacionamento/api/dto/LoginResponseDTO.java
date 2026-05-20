package br.com.estacionamento.api.dto;

public class LoginResponseDTO {
    private String token;
    private Long id;
    private String email;
    private String role;

    public LoginResponseDTO(String token, Long id, String email, String role) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public String getToken() { return token; }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}