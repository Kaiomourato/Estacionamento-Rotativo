package br.com.estacionamento.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.estacionamento.api.dto.LoginRequestDTO;
import br.com.estacionamento.api.dto.LoginResponseDTO;
import br.com.estacionamento.api.dto.RegisterRequestDTO;
import br.com.estacionamento.api.infra.security.TokenService;
import br.com.estacionamento.api.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        // 1. O service valida a senha e retorna o usuário
        br.com.estacionamento.api.model.Usuario usuario = authService.login(dto);

        // 2. Geramos o token
        String token = tokenService.gerarToken(usuario);

        // 3. Retornamos o DTO completo
        return ResponseEntity.ok(new LoginResponseDTO(
            token,
            usuario.getId(),
            usuario.getEmail(),
            usuario.getRole()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequestDTO dto) {
        authService.register(dto);
        return ResponseEntity.status(201).body("Usuário cadastrado com sucesso");
    }

    // JWT é stateless — não há sessão para invalidar no servidor. Esta rota existe
    // apenas para que o logout vire um evento de auditoria (LogAcessoFilter registra
    // toda requisição), com o e-mail/role capturados a partir do próprio token enviado.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}