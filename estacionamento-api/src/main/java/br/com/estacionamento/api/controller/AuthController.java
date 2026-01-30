package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.dto.LoginRequestDTO;
import br.com.estacionamento.api.dto.RegisterRequestDTO;
import br.com.estacionamento.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    public AuthController(AuthenticationManager authenticationManager,
                          AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<br.com.estacionamento.api.dto.LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                dto.getEmail(),
                dto.getSenha()
            )
        );

        br.com.estacionamento.api.model.Usuario usuario = authService.buscarPorEmail(dto.getEmail());

        return ResponseEntity.ok(new br.com.estacionamento.api.dto.LoginResponseDTO(
            usuario.getId(),
            usuario.getEmail(), // Usando email como nome por enquanto
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
