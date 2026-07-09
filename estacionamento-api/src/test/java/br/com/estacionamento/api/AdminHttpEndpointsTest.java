package br.com.estacionamento.api;

import br.com.estacionamento.api.infra.security.TokenService;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.model.TipoVeiculo;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.model.Veiculo;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.EstadiaRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import br.com.estacionamento.api.repository.VagaRepository;
import br.com.estacionamento.api.repository.VeiculoRepository;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe o servidor embarcado de verdade (porta aleatória) e faz requisições
 * HTTP reais com um JWT de ADMIN válido — a única forma de pegar um bug que
 * só existe na camada de MVC/segurança (binding de Pageable pelo Spring, ou
 * o próprio @PreAuthorize), já que os testes de serviço chamam os métodos
 * diretamente e não passam por essa camada.
 */
// @Transactional não é suficiente aqui: o TestRestTemplate faz uma chamada HTTP
// real contra o servidor embarcado, que atende na thread do Tomcat (fora da
// transação de teste), então o rollback automático não alcança essas escritas.
// Por isso o @BeforeEach limpa as tabelas manualmente antes de cada teste, e
// @DirtiesContext fecha o DataSource ao final da classe — o H2 em memória é
// destruído junto (sem DB_CLOSE_DELAY), garantindo que nenhuma linha commitada
// aqui vaze para outras classes de teste que rodem depois nesta mesma JVM.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminHttpEndpointsTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private EstacionamentoRepository estacionamentoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private VeiculoRepository veiculoRepository;
    @Autowired
    private VagaRepository vagaRepository;
    @Autowired
    private EstadiaRepository estadiaRepository;

    private String tokenAdmin;

    // Limpa antes E depois: o H2 em memória "testdb" é identificado só pelo nome, e o
    // Spring Test costuma manter em cache um ApplicationContext MOCK (usado pelos outros
    // testes da suíte) com sua própria conexão aberta a esse mesmo "testdb" — então o
    // DB_CLOSE_DELAY=0 não garante que o banco seja zerado só porque o contexto RANDOM_PORT
    // deste teste foi encerrado. Sem o cleanup no @AfterEach, uma linha commitada aqui via
    // HTTP real (fora de transação de teste) sobrevive para a próxima classe de teste.
    @BeforeEach
    void setUp() {
        limparTabelas();

        Estacionamento estacionamento = estacionamentoRepository.save(
                new Estacionamento("Estacionamento HTTP Teste", "Rua C", -5.0, -42.0, 5.0));

        Usuario admin = new Usuario("admin.http@teste.com", "senha123", "ADMIN");
        usuarioRepository.save(admin);
        tokenAdmin = tokenService.gerarToken(admin);

        Usuario motorista = usuarioRepository.save(new Usuario("motorista.http@teste.com", "senha123", "USER"));

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca("HTTP123");
        veiculo.setTipo(TipoVeiculo.CARRO);
        veiculo.setUsuario(motorista);
        veiculo = veiculoRepository.save(veiculo);

        Vaga vaga = new Vaga("HTTP-1", false);
        vaga.setEstacionamento(estacionamento);
        vaga = vagaRepository.save(vaga);

        Estadia estadia = new Estadia();
        estadia.setVeiculo(veiculo);
        estadia.setVaga(vaga);
        estadia.setEntrada(LocalDateTime.now().minusHours(1));
        estadia.setSaida(LocalDateTime.now());
        estadia.setAtiva(false);
        estadia.setPendente(false);
        estadia.setValor(5.0);
        estadia.setCriadoEm(LocalDateTime.now().minusHours(1));
        estadiaRepository.save(estadia);
    }

    @AfterEach
    void tearDown() {
        limparTabelas();
    }

    private void limparTabelas() {
        estadiaRepository.deleteAll();
        vagaRepository.deleteAll();
        veiculoRepository.deleteAll();
        usuarioRepository.deleteAll();
        estacionamentoRepository.deleteAll();
    }

    private HttpEntity<Void> comTokenAdmin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenAdmin);
        return new HttpEntity<>(headers);
    }

    @Test
    void getAdminUsuariosRetorna200EPaginaValida() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/admin/usuarios?page=0&size=15", HttpMethod.GET, comTokenAdmin(), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"content\"").contains("admin.http@teste.com");
    }

    @Test
    void getAdminOperadoresComFiltroRoleRetorna200() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/admin/usuarios?role=OPERADOR&page=0&size=15", HttpMethod.GET, comTokenAdmin(), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAdminVagasRetorna200EPaginaValida() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/admin/vagas?page=0&size=15", HttpMethod.GET, comTokenAdmin(), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("HTTP-1");
    }

    @Test
    void getAdminPagamentosRetorna200EPaginaValida() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/admin/pagamentos?page=0&size=15", HttpMethod.GET, comTokenAdmin(), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("HTTP123");
    }

    @Test
    void getAdminAuditoriaRetorna200() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/admin/auditoria?page=0&size=20", HttpMethod.GET, comTokenAdmin(), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAdminAuditoriaTiposEventoRetorna200() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/admin/auditoria/tipos-evento", HttpMethod.GET, comTokenAdmin(), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("LOGIN");
    }

    @Test
    void semTokenRetorna401Ou403ParaRotasAdmin() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/admin/usuarios", HttpMethod.GET, HttpEntity.EMPTY, String.class);

        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }
}
