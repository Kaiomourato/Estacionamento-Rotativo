package br.com.estacionamento.api;

import br.com.estacionamento.api.infra.security.TokenService;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.TipoVeiculo;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.FavoritoRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import br.com.estacionamento.api.repository.VagaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe o servidor embarcado de verdade (porta aleatória) e exercita /favoritos com um
 * JWT de motorista válido, incluindo a camada de segurança e serialização JSON — os
 * testes de FavoritoService chamam os métodos diretamente e não passam por essa camada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FavoritoHttpEndpointsTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private TokenService tokenService;
    @Autowired private EstacionamentoRepository estacionamentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VagaRepository vagaRepository;
    @Autowired private FavoritoRepository favoritoRepository;

    private String tokenMotorista;
    private Estacionamento estacionamento;

    @BeforeEach
    void setUp() {
        limparTabelas();

        estacionamento = estacionamentoRepository.save(
                new Estacionamento("Estacionamento HTTP Favoritos", "Rua D", -5.0, -42.0, 4.5));

        Vaga vaga = new Vaga("F-1", false);
        vaga.setEstacionamento(estacionamento);
        vaga.setTipoVeiculo(TipoVeiculo.CARRO);
        vagaRepository.save(vaga);

        Usuario motorista = usuarioRepository.save(new Usuario("motorista.favoritos@teste.com", "senha123", "USER"));
        tokenMotorista = tokenService.gerarToken(motorista);
    }

    @AfterEach
    void tearDown() {
        limparTabelas();
    }

    private void limparTabelas() {
        favoritoRepository.deleteAll();
        vagaRepository.deleteAll();
        usuarioRepository.deleteAll();
        estacionamentoRepository.deleteAll();
    }

    private HttpEntity<Void> comToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenMotorista);
        return new HttpEntity<>(headers);
    }

    @Test
    void fluxoCompletoDeFavoritarListarERemover() {
        ResponseEntity<String> vazio = restTemplate.exchange(
                "/favoritos/meus", HttpMethod.GET, comToken(), String.class);
        assertThat(vazio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(vazio.getBody()).isEqualTo("[]");

        ResponseEntity<String> add = restTemplate.exchange(
                "/favoritos/" + estacionamento.getId(), HttpMethod.POST, comToken(), String.class);
        assertThat(add.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(add.getBody()).contains("Estacionamento HTTP Favoritos");
        // Nunca deve vazar o usuário dono do favorito no JSON
        assertThat(add.getBody()).doesNotContain("motorista.favoritos@teste.com");

        ResponseEntity<String> addDeNovo = restTemplate.exchange(
                "/favoritos/" + estacionamento.getId(), HttpMethod.POST, comToken(), String.class);
        assertThat(addDeNovo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(favoritoRepository.findByUsuarioEmail("motorista.favoritos@teste.com")).hasSize(1);

        ResponseEntity<String> listado = restTemplate.exchange(
                "/favoritos/meus", HttpMethod.GET, comToken(), String.class);
        assertThat(listado.getBody()).contains("Estacionamento HTTP Favoritos");

        ResponseEntity<Void> remover = restTemplate.exchange(
                "/favoritos/" + estacionamento.getId(), HttpMethod.DELETE, comToken(), Void.class);
        assertThat(remover.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Void> removerDeNovo = restTemplate.exchange(
                "/favoritos/" + estacionamento.getId(), HttpMethod.DELETE, comToken(), Void.class);
        assertThat(removerDeNovo.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> vazioDeNovo = restTemplate.exchange(
                "/favoritos/meus", HttpMethod.GET, comToken(), String.class);
        assertThat(vazioDeNovo.getBody()).isEqualTo("[]");
    }

    @Test
    void semTokenRetorna401Ou403ParaFavoritos() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/favoritos/meus", HttpMethod.GET, HttpEntity.EMPTY, String.class);

        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    void listaPublicaDeEstacionamentosExpoeTiposAceitos() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/estacionamentos", HttpMethod.GET, HttpEntity.EMPTY, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("tiposAceitos").contains("CARRO");
    }
}
