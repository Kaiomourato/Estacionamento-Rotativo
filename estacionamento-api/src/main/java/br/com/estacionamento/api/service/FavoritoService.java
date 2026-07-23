package br.com.estacionamento.api.service;

import br.com.estacionamento.api.exception.RecursoNaoEncontradoException;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Favorito;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.FavoritoRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FavoritoService {

    private final FavoritoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final EstacionamentoRepository estacionamentoRepository;

    public FavoritoService(FavoritoRepository repository, UsuarioRepository usuarioRepository,
                            EstacionamentoRepository estacionamentoRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.estacionamentoRepository = estacionamentoRepository;
    }

    public List<Estacionamento> listarPorUsuario(String email) {
        return repository.findByUsuarioEmail(email).stream()
                .map(Favorito::getEstacionamento)
                .toList();
    }

    @Transactional
    public Favorito adicionar(String email, Long estacionamentoId) {
        return repository.findByUsuarioEmailAndEstacionamentoId(email, estacionamentoId)
                .orElseGet(() -> criarFavorito(email, estacionamentoId));
    }

    private Favorito criarFavorito(String email, Long estacionamentoId) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado no banco de dados."));

        Estacionamento estacionamento = estacionamentoRepository.findById(estacionamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estacionamento não encontrado com o ID: " + estacionamentoId));

        Favorito favorito = new Favorito();
        favorito.setUsuario(usuario);
        favorito.setEstacionamento(estacionamento);
        favorito.setCriadoEm(LocalDateTime.now());
        return repository.save(favorito);
    }

    @Transactional
    public void remover(String email, Long estacionamentoId) {
        repository.findByUsuarioEmailAndEstacionamentoId(email, estacionamentoId)
                .ifPresent(repository::delete);
    }
}
