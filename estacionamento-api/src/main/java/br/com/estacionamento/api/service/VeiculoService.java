package br.com.estacionamento.api.service;

import br.com.estacionamento.api.exception.RecursoNaoEncontradoException;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.model.Veiculo;
import br.com.estacionamento.api.repository.UsuarioRepository;
import br.com.estacionamento.api.repository.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VeiculoService {

    private final VeiculoRepository repository;
    private final UsuarioRepository usuarioRepository;

    // Injeção via construtor (melhor prática que @Autowired em campo)
    public VeiculoService(VeiculoRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Veiculo cadastrarComUsuario(Veiculo veiculo, String email) {
        // 1. Busca o usuário que está logado
        Usuario dono = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado no banco de dados."));

        // 2. Verifica se a placa já está cadastrada para evitar duplicidade
        if (repository.existsByPlaca(veiculo.getPlaca())) {
            throw new RuntimeException("Já existe um veículo cadastrado com esta placa.");
        }

        // 3. Vincula o dono ao veículo
        veiculo.setUsuario(dono);
        
        // 4. Garante que o veículo comece como ativo
        veiculo.setAtivo(true);

        return repository.save(veiculo);
    }

    public List<Veiculo> listarPorUsuario(String email) {
        // Busca apenas os veículos onde o email do usuário vinculado seja o fornecido
        return repository.findByUsuarioEmail(email);
    }

    public List<Veiculo> listarTodos() {
        return repository.findAll();
    }

    public Veiculo buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado com o ID: " + id));
    }

    @Transactional
    public void desativar(Long id) {
        Veiculo veiculo = buscarPorId(id);
        veiculo.setAtivo(false);
        repository.save(veiculo);
    }
}