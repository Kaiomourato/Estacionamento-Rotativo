package br.com.estacionamento.api;

import br.com.estacionamento.api.dto.UsuarioResumoDTO;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.model.LogAcesso;
import br.com.estacionamento.api.model.TipoEventoLog;
import br.com.estacionamento.api.model.TipoVeiculo;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.model.Veiculo;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.EstadiaRepository;
import br.com.estacionamento.api.repository.LogAcessoRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import br.com.estacionamento.api.repository.VagaRepository;
import br.com.estacionamento.api.repository.VeiculoRepository;
import br.com.estacionamento.api.service.AdminDashboardService;
import br.com.estacionamento.api.service.AuditoriaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Exercita de ponta a ponta (repositório -> service -> serialização JSON) as
 * páginas administrativas que usam Page<T>/Pageable — Usuários, Vagas,
 * Pagamentos e Auditoria — para pegar qualquer exceção em tempo de execução
 * (ex.: LazyInitializationException, erro de tradução JPQL->SQL) que só
 * aparece com uma sessão Hibernate real, algo que os testes existentes não
 * cobriam (nenhum deles chama esses métodos).
 */
@SpringBootTest
@Transactional
class AdminEndpointsTest {

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
    @Autowired
    private LogAcessoRepository logAcessoRepository;
    @Autowired
    private AdminDashboardService adminDashboardService;
    @Autowired
    private AuditoriaService auditoriaService;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Estacionamento estacionamento = estacionamentoRepository.save(
                new Estacionamento("Estacionamento ADM Teste", "Rua B", -5.0, -42.0, 5.0));

        Usuario admin = new Usuario("admin@teste.com", "senha123", "ADMIN");
        usuarioRepository.save(admin);

        Usuario operador = new Usuario("operador.adm@teste.com", "senha123", "OPERADOR");
        operador.setEstacionamento(estacionamento);
        usuarioRepository.save(operador);

        Usuario motorista = new Usuario("motorista.adm@teste.com", "senha123", "USER");
        motorista = usuarioRepository.save(motorista);

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca("ADM1234");
        veiculo.setTipo(TipoVeiculo.CARRO);
        veiculo.setUsuario(motorista);
        veiculo = veiculoRepository.save(veiculo);

        Vaga vaga = new Vaga("ADM-1", false);
        vaga.setEstacionamento(estacionamento);
        vaga = vagaRepository.save(vaga);

        Estadia estadia = new Estadia();
        estadia.setVeiculo(veiculo);
        estadia.setVaga(vaga);
        estadia.setEntrada(LocalDateTime.now().minusHours(2));
        estadia.setSaida(LocalDateTime.now());
        estadia.setAtiva(false);
        estadia.setPendente(false);
        estadia.setValor(15.0);
        estadia.setCriadoEm(LocalDateTime.now().minusHours(2));
        estadiaRepository.save(estadia);

        LogAcesso log = new LogAcesso("admin@teste.com", "GET", "/admin/usuarios", 200, LocalDateTime.now());
        log.setRole("ADMIN");
        log.setTipoEvento(TipoEventoLog.ACESSO_PAINEL_ADMIN);
        log.setDescricao("Acesso ao painel administrativo");
        logAcessoRepository.save(log);
    }

    @Test
    void listagemDeUsuariosPaginadaNaoFalhaESerializaCorretamente() throws Exception {
        Page<UsuarioResumoDTO> pagina = adminDashboardService.listarUsuariosPaginado(null, null, PageRequest.of(0, 15));

        assertThat(pagina.getTotalElements()).isGreaterThanOrEqualTo(3);
        String json = objectMapper.writeValueAsString(pagina);
        assertThat(json).contains("admin@teste.com");
    }

    @Test
    void listagemDeUsuariosFiltradaPorRoleOperadorFunciona() {
        Page<UsuarioResumoDTO> pagina = adminDashboardService.listarUsuariosPaginado("OPERADOR", null, PageRequest.of(0, 15));

        assertThat(pagina.getContent()).extracting(UsuarioResumoDTO::getEmail).contains("operador.adm@teste.com");
    }

    @Test
    void listagemDeVagasPaginadaNaoFalhaESerializaCorretamente() throws Exception {
        Page<Vaga> pagina = adminDashboardService.listarVagasPaginado(null, null, PageRequest.of(0, 15));

        assertThat(pagina.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThatCode(() -> objectMapper.writeValueAsString(pagina)).doesNotThrowAnyException();
    }

    @Test
    void listagemDePagamentosPaginadaNaoFalhaESerializaCorretamente() throws Exception {
        Page<Estadia> pagina = adminDashboardService.listarPagamentosPaginado(null, null, null, PageRequest.of(0, 15));

        assertThat(pagina.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThatCode(() -> objectMapper.writeValueAsString(pagina)).doesNotThrowAnyException();
    }

    @Test
    void auditoriaPaginadaNaoFalhaESerializaCorretamente() throws Exception {
        Page<LogAcesso> pagina = auditoriaService.buscar(null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(pagina.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThatCode(() -> objectMapper.writeValueAsString(pagina)).doesNotThrowAnyException();
    }

    @Test
    void auditoriaComFiltroDeTipoEventoFunciona() {
        Page<LogAcesso> pagina = auditoriaService.buscar(null, null, null, "ACESSO_PAINEL_ADMIN", null, null, null, PageRequest.of(0, 20));

        assertThat(pagina.getContent()).isNotEmpty();
    }
}
