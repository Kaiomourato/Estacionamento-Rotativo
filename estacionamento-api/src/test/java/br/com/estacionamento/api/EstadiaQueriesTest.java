package br.com.estacionamento.api;

import br.com.estacionamento.api.dto.RelatorioOperadorDTO;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.model.StatusEstadia;
import br.com.estacionamento.api.model.TipoVeiculo;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.model.Veiculo;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.EstadiaRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import br.com.estacionamento.api.repository.VagaRepository;
import br.com.estacionamento.api.repository.VeiculoRepository;
import br.com.estacionamento.api.service.EstadiaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduz, com o esquema atual das entidades aplicado num H2 "do zero", as
 * consultas por trás dos endpoints relatados como quebrados (bugs #3, #4,
 * #5, #6, #7 e #8). Cada teste falharia com "column ... not found" antes do
 * SchemaPatchRunner garantir as colunas novas de Estadia, ou exporia campos
 * indevidos no JSON antes do ajuste de @JsonIgnoreProperties em Vaga.
 */
@SpringBootTest
@Transactional
class EstadiaQueriesTest {

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
    private EstadiaService estadiaService;
    @Autowired
    private ObjectMapper objectMapper;

    private Usuario motorista;
    private Veiculo veiculo;
    private Vaga vaga;

    @BeforeEach
    void setUp() {
        Estacionamento estacionamento = estacionamentoRepository.save(
                new Estacionamento("Estacionamento Teste", "Rua A", -5.0, -42.0, 5.0));

        Usuario operador = new Usuario("operador@teste.com", "senha123", "OPERADOR");
        operador.setEstacionamento(estacionamento);
        usuarioRepository.save(operador);

        motorista = new Usuario("motorista@teste.com", "senha123", "USER");
        motorista = usuarioRepository.save(motorista);

        veiculo = new Veiculo();
        veiculo.setPlaca("ABC1234");
        veiculo.setTipo(TipoVeiculo.CARRO);
        veiculo.setUsuario(motorista);
        veiculo = veiculoRepository.save(veiculo);

        vaga = new Vaga("A1", false);
        vaga.setEstacionamento(estacionamento);
        vaga = vagaRepository.save(vaga);
    }

    private Estadia novaEstadia() {
        Estadia estadia = new Estadia();
        estadia.setVeiculo(veiculo);
        estadia.setVaga(vaga);
        estadia.setCriadoEm(LocalDateTime.now());
        return estadia;
    }

    // Bug #5: GET /estadias/minha-ativa
    @Test
    void buscaEstadiaAtivaDoMotorista() {
        Estadia estadia = novaEstadia();
        estadia.setEntrada(LocalDateTime.now());
        estadia.setAtiva(true);
        estadia.setPendente(true);
        estadia.setCodigo("AAA111");
        estadiaRepository.save(estadia);

        Estadia ativa = estadiaService.buscarEstadiaAtivaDoMotorista(motorista.getEmail());

        assertThat(ativa.getCodigo()).isEqualTo("AAA111");
    }

    // Bug #4: POST /estadias/reservar (verificação de estadia ativa antes de reservar)
    @Test
    void verificaEstadiaAtivaAntesDeReservar() {
        Estadia estadia = novaEstadia();
        estadia.setEntrada(LocalDateTime.now());
        estadia.setAtiva(true);
        estadia.setPendente(true);
        estadia.setCodigo("BBB222");
        estadiaRepository.save(estadia);

        Optional<Estadia> ativa = estadiaRepository.findByAtivaTrueAndVeiculoUsuarioEmail(motorista.getEmail());

        assertThat(ativa).isPresent();
    }

    // Bug #8: GET /estadias/ativas
    @Test
    void listaEstadiasAtivasDoOperador() {
        Estadia estadia = novaEstadia();
        estadia.setEntrada(LocalDateTime.now());
        estadia.setAtiva(true);
        estadia.setPendente(false);
        estadiaRepository.save(estadia);

        List<Estadia> ativas = estadiaService.listarAtivasPorOperador("operador@teste.com");

        assertThat(ativas).hasSize(1);
    }

    // Bug #6: GET /estadias/meu-historico
    @Test
    void historicoDoMotoristaListaEstadiasEncerradas() {
        Estadia estadia = novaEstadia();
        estadia.setEntrada(LocalDateTime.now().minusHours(2));
        estadia.setSaida(LocalDateTime.now());
        estadia.setAtiva(false);
        estadia.setPendente(false);
        estadia.setValor(10.0);
        estadiaRepository.save(estadia);

        List<Estadia> historico = estadiaService.buscarHistoricoDoMotorista(motorista.getEmail());

        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).getStatus()).isEqualTo(StatusEstadia.FINALIZADA);
    }

    // GET /estadias/historico (operador): faltava endpoint/consulta para o
    // histórico de estadias encerradas do estacionamento do operador.
    @Test
    void historicoDoOperadorListaEstadiasEncerradas() {
        Estadia estadia = novaEstadia();
        estadia.setEntrada(LocalDateTime.now().minusHours(2));
        estadia.setSaida(LocalDateTime.now());
        estadia.setAtiva(false);
        estadia.setPendente(false);
        estadia.setValor(10.0);
        estadiaRepository.save(estadia);

        List<Estadia> historico = estadiaService.listarHistoricoPorOperador("operador@teste.com");

        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).getStatus()).isEqualTo(StatusEstadia.FINALIZADA);
    }

    // Bug #7: GET /estacionamentos/meu/relatorio
    @Test
    void relatorioOperadorNaoFalha() {
        RelatorioOperadorDTO relatorio = estadiaService.gerarRelatorio("operador@teste.com");

        assertThat(relatorio).isNotNull();
        assertThat(relatorio.getFaturamento()).isNotNull();
        assertThat(relatorio.getFaturamento().getAberto()).isEqualTo(0.0);
        assertThat(relatorio.getFluxoSemanal()).hasSize(7);
        assertThat(relatorio.getFaturamentoMensal()).hasSize(12);
    }

    // Bug #3: GET /vagas não deve diferir de /vagas/resumo por causa de campos
    // calculados de Estacionamento vazando (e potencialmente disparando lazy load)
    @Test
    void serializacaoDeVagaNaoExpoeContadoresDoEstacionamento() throws Exception {
        Vaga vagaCarregada = vagaRepository.findById(vaga.getId()).orElseThrow();

        String json = objectMapper.writeValueAsString(vagaCarregada);

        assertThat(json).doesNotContain("vagasTotais", "vagasOcupadas", "vagasLivres");
    }
}
