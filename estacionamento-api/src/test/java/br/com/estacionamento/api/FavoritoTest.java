package br.com.estacionamento.api;

import br.com.estacionamento.api.exception.RecursoNaoEncontradoException;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Favorito;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.FavoritoRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import br.com.estacionamento.api.service.FavoritoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class FavoritoTest {

    @Autowired private EstacionamentoRepository estacionamentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private FavoritoService favoritoService;

    private Usuario motorista;
    private Estacionamento estacionamento;

    @BeforeEach
    void setUp() {
        estacionamento = estacionamentoRepository.save(
                new Estacionamento("Estacionamento Teste", "Rua A", -5.0, -42.0, 5.0));

        motorista = usuarioRepository.save(new Usuario("motorista@teste.com", "senha123", "USER"));
    }

    @Test
    void adicionarCriaFavoritoParaOUsuarioLogado() {
        Favorito favorito = favoritoService.adicionar(motorista.getEmail(), estacionamento.getId());

        assertThat(favorito.getId()).isNotNull();
        assertThat(favorito.getEstacionamento().getId()).isEqualTo(estacionamento.getId());
        assertThat(favoritoRepository.existsByUsuarioEmailAndEstacionamentoId(motorista.getEmail(), estacionamento.getId())).isTrue();
    }

    @Test
    void adicionarDuasVezesNaoDuplicaOFavorito() {
        favoritoService.adicionar(motorista.getEmail(), estacionamento.getId());
        favoritoService.adicionar(motorista.getEmail(), estacionamento.getId());

        List<Favorito> favoritos = favoritoRepository.findByUsuarioEmail(motorista.getEmail());
        assertThat(favoritos).hasSize(1);
    }

    @Test
    void adicionarComEstacionamentoInexistenteLancaExcecao() {
        assertThatThrownBy(() -> favoritoService.adicionar(motorista.getEmail(), 999L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listarPorUsuarioRetornaApenasOsFavoritosDoProprioUsuario() {
        Usuario outroMotorista = usuarioRepository.save(new Usuario("outro@teste.com", "senha123", "USER"));
        favoritoService.adicionar(motorista.getEmail(), estacionamento.getId());
        favoritoService.adicionar(outroMotorista.getEmail(), estacionamento.getId());

        List<Estacionamento> favoritosDoMotorista = favoritoService.listarPorUsuario(motorista.getEmail());

        assertThat(favoritosDoMotorista).hasSize(1);
        assertThat(favoritosDoMotorista.get(0).getId()).isEqualTo(estacionamento.getId());
    }

    @Test
    void removerApagaOFavorito() {
        favoritoService.adicionar(motorista.getEmail(), estacionamento.getId());

        favoritoService.remover(motorista.getEmail(), estacionamento.getId());

        assertThat(favoritoRepository.existsByUsuarioEmailAndEstacionamentoId(motorista.getEmail(), estacionamento.getId())).isFalse();
    }

    @Test
    void removerUmFavoritoInexistenteNaoLancaExcecao() {
        assertThat(favoritoRepository.findByUsuarioEmailAndEstacionamentoId(motorista.getEmail(), estacionamento.getId())).isEmpty();

        favoritoService.remover(motorista.getEmail(), estacionamento.getId());
    }
}
