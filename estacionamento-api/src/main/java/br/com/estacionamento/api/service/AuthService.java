package br.com.estacionamento.api.service;

import br.com.estacionamento.api.dto.LoginRequestDTO;
import br.com.estacionamento.api.dto.RegisterRequestDTO;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cadastro de usuário
     */
    public void register(RegisterRequestDTO dto) {

        // 1️⃣ Verifica se o e-mail já existe
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("E-mail já cadastrado");
        }

        // 2️⃣ Criptografa a senha
        String senhaCriptografada = passwordEncoder.encode(dto.getSenha());

        // 3️⃣ Cria o usuário
        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(senhaCriptografada);
        usuario.setRole(dto.getRole() != null ? dto.getRole() : "USER");

        // 4️⃣ Salva no banco
        usuarioRepository.save(usuario);
    }

    /**
     * Login do usuário
     */
    public void login(LoginRequestDTO dto) {

        // 1️⃣ Busca usuário pelo e-mail
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2️⃣ Verifica a senha
        boolean senhaValida = passwordEncoder.matches(
                dto.getSenha(),
                usuario.getSenha()
        );

        if (!senhaValida) {
            throw new RuntimeException("Senha inválida");
        }

    
    }
}
