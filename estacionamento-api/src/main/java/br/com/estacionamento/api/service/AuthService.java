package br.com.estacionamento.api.service;

import br.com.estacionamento.api.dto.LoginRequestDTO;
import br.com.estacionamento.api.dto.RegisterRequestDTO;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

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

        // Se a role não for enviada, assume o padrão "USER" (Motorista)
        if (dto.getRole() == null || dto.getRole().isEmpty()) {
            usuario.setRole("USER");
        } else {
            usuario.setRole(dto.getRole().toUpperCase());
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
        Usuario usuario = buscarPorEmail(dto.getEmail());

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            throw new RuntimeException("Senha inválida");
        }
        return usuario;
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}