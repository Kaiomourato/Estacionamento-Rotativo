package br.com.estacionamento.api;

import br.com.estacionamento.api.exception.RecursoNaoEncontradoException;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.model.Notificacao;
import br.com.estacionamento.api.model.TipoNotificacao;
import br.com.estacionamento.api.model.TipoVeiculo;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.model.Veiculo;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.EstadiaRepository;
import br.com.estacionamento.api.repository.NotificacaoRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import br.com.estacionamento.api.repository.VagaRepository;
import br.com.estacionamento.api.repository.VeiculoRepository;
import br.com.estacionamento.api.service.EstadiaService;
import br.com.estacionamento.api.service.NotificacaoScheduler;
import br.com.estacionamento.api.service.NotificacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class NotificacaoTest {

    @Autowired private EstacionamentoRepository estacionamentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VeiculoRepository veiculoRepository;
    @Autowired private VagaRepository vagaRepository;
    @Autowired private EstadiaRepository estadiaRepository;
    @Autowired private EstadiaService estadiaService;
    @Autowired private NotificacaoRepository notificacaoRepository;
    @Autowired private NotificacaoService notificacaoService;
    @Autowired private NotificacaoScheduler notificacaoScheduler;

    private Usuario motorista;
    private Veiculo veiculo;
    private Vaga vaga;
    private Estacionamento estacionamento;
    private static final String EMAIL_OPERADOR = "operador@teste.com";

    @BeforeEach
    void setUp() {
        estacionamento = estacionamentoRepository.save(
                new Estacionamento("Estacionamento Teste", "Rua A", -5.0, -42.0, 5.0));

        Usuario operador = new Usuario(EMAIL_OPERADOR, "senha123", "OPERADOR");
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

    private Estadia reservar() {
        return estadiaService.reservarVaga(motorista.getEmail(), vaga.getId(), veiculo.getId(), null);
    }

    @Test
    void cancelarReservaNotificaMotorista() {
        Estadia reserva = reservar();

        estadiaService.cancelarReserva(EMAIL_OPERADOR, reserva.getId());

        List<Notificacao> notificacoes = notificacaoRepository.findByDestinatarioEmailOrderByCriadoEmDesc(motorista.getEmail());
        assertThat(notificacoes).anyMatch(n -> n.getTipo() == TipoNotificacao.RESERVA_CANCELADA
                && n.getEstadiaId().equals(reserva.getId()));
    }

    @Test
    void confirmarCheckinNotificaMotorista() {
        Estadia reserva = reservar();

        estadiaService.confirmarCheckin(EMAIL_OPERADOR, reserva.getCodigo());

        List<Notificacao> notificacoes = notificacaoRepository.findByDestinatarioEmailOrderByCriadoEmDesc(motorista.getEmail());
        assertThat(notificacoes).anyMatch(n -> n.getTipo() == TipoNotificacao.CHECKIN_CONFIRMADO
                && n.getEstadiaId().equals(reserva.getId()));
    }

    @Test
    void finalizarEstadiaNotificaMotoristaComValor() {
        Estadia reserva = reservar();
        Estadia emUso = estadiaService.confirmarCheckin(EMAIL_OPERADOR, reserva.getCodigo());
        emUso.setEntrada(LocalDateTime.now().minusHours(2));
        estadiaRepository.save(emUso);

        estadiaService.finalizarEstadia(emUso.getId());

        List<Notificacao> notificacoes = notificacaoRepository.findByDestinatarioEmailOrderByCriadoEmDesc(motorista.getEmail());
        assertThat(notificacoes).anyMatch(n -> n.getTipo() == TipoNotificacao.ESTADIA_FINALIZADA
                && n.getEstadiaId().equals(emUso.getId())
                && n.getMensagem().contains("pagar"));
    }

    @Test
    void reservarVagaNotificaOperador() {
        Estadia reserva = reservar();

        List<Notificacao> notificacoes = notificacaoRepository.findByDestinatarioEmailOrderByCriadoEmDesc(EMAIL_OPERADOR);
        assertThat(notificacoes).anyMatch(n -> n.getTipo() == TipoNotificacao.NOVA_RESERVA
                && n.getEstadiaId().equals(reserva.getId()));
    }

    @Test
    void marcarComoLidaDeOutroUsuarioLancaExcecao() {
        Notificacao notificacao = notificacaoService.criar(motorista, TipoNotificacao.CHECKIN_CONFIRMADO,
                "Titulo", "Mensagem", null);

        assertThatThrownBy(() -> notificacaoService.marcarComoLida("outro@teste.com", notificacao.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void marcarComoLidaDoProprioUsuarioFunciona() {
        Notificacao notificacao = notificacaoService.criar(motorista, TipoNotificacao.CHECKIN_CONFIRMADO,
                "Titulo", "Mensagem", null);

        notificacaoService.marcarComoLida(motorista.getEmail(), notificacao.getId());

        Notificacao atualizada = notificacaoRepository.findById(notificacao.getId()).orElseThrow();
        assertThat(atualizada.isLida()).isTrue();
    }

    @Test
    void contaNaoLidasCorretamente() {
        notificacaoService.criar(motorista, TipoNotificacao.CHECKIN_CONFIRMADO, "T1", "M1", null);
        notificacaoService.criar(motorista, TipoNotificacao.RESERVA_CANCELADA, "T2", "M2", null);

        assertThat(notificacaoService.contarNaoLidas(motorista.getEmail())).isEqualTo(2);

        notificacaoService.marcarTodasComoLidas(motorista.getEmail());

        assertThat(notificacaoService.contarNaoLidas(motorista.getEmail())).isEqualTo(0);
    }

    @Test
    void schedulerNaoDuplicaNotificacaoDeReservaExpirando() {
        Estadia reserva = reservar();
        reserva.setPrevisaoChegada(LocalDateTime.now().plusMinutes(5));
        estadiaRepository.save(reserva);

        notificacaoScheduler.verificarReservasPendentes();
        notificacaoScheduler.verificarReservasPendentes();

        long total = notificacaoRepository.findByDestinatarioEmailOrderByCriadoEmDesc(motorista.getEmail()).stream()
                .filter(n -> n.getTipo() == TipoNotificacao.RESERVA_EXPIRANDO && n.getEstadiaId().equals(reserva.getId()))
                .count();
        assertThat(total).isEqualTo(1);
    }

    @Test
    void schedulerNaoDuplicaNotificacaoDeCheckinAtrasado() {
        Estadia reserva = reservar();
        reserva.setPrevisaoChegada(LocalDateTime.now().minusMinutes(40));
        estadiaRepository.save(reserva);

        notificacaoScheduler.verificarReservasPendentes();
        notificacaoScheduler.verificarReservasPendentes();

        long total = notificacaoRepository.findByDestinatarioEmailOrderByCriadoEmDesc(EMAIL_OPERADOR).stream()
                .filter(n -> n.getTipo() == TipoNotificacao.CHECKIN_ATRASADO && n.getEstadiaId().equals(reserva.getId()))
                .count();
        assertThat(total).isEqualTo(1);
    }
}
