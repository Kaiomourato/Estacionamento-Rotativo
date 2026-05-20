package br.com.estacionamento.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.estacionamento.api.dto.LoginRequestDTO;
import br.com.estacionamento.api.dto.LoginResponseDTO;
import br.com.estacionamento.api.dto.RegisterRequestDTO;
import br.com.estacionamento.api.infra.security.TokenService;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, AuthService authService, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getSenha());
        var authentication = authenticationManager.authenticate(authenticationToken);

        var usuario = (Usuario) authentication.getPrincipal();
        var token = tokenService.gerarToken(usuario);

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
}