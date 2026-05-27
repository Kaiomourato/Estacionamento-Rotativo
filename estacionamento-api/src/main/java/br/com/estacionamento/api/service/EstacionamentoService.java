package br.com.estacionamento.api.service;

import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstacionamentoService {

    private final EstacionamentoRepository repository;
    private final UsuarioRepository usuarioRepository; // NOVO

    public EstacionamentoService(EstacionamentoRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Estacionamento> listarTodos() {
        return repository.findAll();
    }

    public List<Estacionamento> buscarProximos(Double lat, Double lng, Double raio) {
        return repository.findProximos(lat, lng, raio);
    }

    public Estacionamento salvar(Estacionamento estacionamento) {
        return repository.save(estacionamento);
    }

    public Estacionamento buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Estacionamento não encontrado"));
    }

    // NOVO: Busca apenas o estacionamento vinculado ao operador logado
    public Estacionamento buscarMeuEstacionamento(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
                
        if (usuario.getEstacionamento() == null) {
            throw new RuntimeException("Este operador não possui um estacionamento vinculado.");
        }
        return usuario.getEstacionamento();
    }

    // NOVO: Atualiza nome e valor da hora (usado na nova aba)
    public Estacionamento atualizar(Long id, Estacionamento dados) {
        Estacionamento est = buscarPorId(id);
        est.setNome(dados.getNome());
        est.setValorHora(dados.getValorHora());
        return repository.save(est);
    }
}