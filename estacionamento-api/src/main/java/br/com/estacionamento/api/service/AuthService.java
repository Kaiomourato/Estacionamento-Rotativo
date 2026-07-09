package br.com.estacionamento.api.service;

import br.com.estacionamento.api.dto.LoginRequestDTO;
import br.com.estacionamento.api.dto.RegisterRequestDTO;
import br.com.estacionamento.api.exception.CredenciaisInvalidasException;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthService {

    private static final Set<String> ROLES_VALIDAS = Set.of("USER", "OPERADOR", "ADMIN");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EstacionamentoRepository estacionamentoRepository; // NOVO

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, EstacionamentoRepository estacionamentoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.estacionamentoRepository = estacionamentoRepository;
    }

    public void register(RegisterRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("E-mail já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setCriadoEm(LocalDateTime.now());

        // Se a role não for enviada, assume o padrão "USER" (Motorista)
        if (dto.getRole() == null || dto.getRole().isBlank()) {
            usuario.setRole("USER");
        } else {
            String roleInformada = dto.getRole().toUpperCase();
            if (!ROLES_VALIDAS.contains(roleInformada)) {
                throw new RuntimeException("Role inválida. Valores aceitos: " + ROLES_VALIDAS);
            }
            usuario.setRole(roleInformada);
        }

        // Se for um OPERADOR, obrigatoriamente vincula a um Estacionamento
        if (usuario.getRole().equals("OPERADOR")) {
            if (dto.getEstacionamentoId() == null) {
                throw new RuntimeException("Um operador precisa estar vinculado a um estacionamento (ID não fornecido).");
            }
            Estacionamento est = estacionamentoRepository.findById(dto.getEstacionamentoId())
                    .orElseThrow(() -> new RuntimeException("Estacionamento não encontrado"));
            
            usuario.setEstacionamento(est);
        }

        usuarioRepository.save(usuario);
    }

    public Usuario login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CredenciaisInvalidasException("Email ou senha inválidos"));

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException("Email ou senha inválidos");
        }
        return usuario;
    }
}