package br.com.estacionamento.api.service;

import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.model.TipoNotificacao;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.EstadiaRepository;
import br.com.estacionamento.api.repository.NotificacaoRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Verifica reservas pendentes (sem check-in) a cada 5 minutos: avisa o motorista quando
// a reserva está prestes a expirar e avisa o operador quando o check-in já está atrasado.
// Cada tipo de notificação só é criado uma vez por estadia (checagem via NotificacaoRepository).
@Component
public class NotificacaoScheduler {

    private static final long LIMITE_EXPIRANDO_MINUTOS = 15;
    private static final long LIMITE_ATRASO_MINUTOS = 30;

    private final EstadiaRepository estadiaRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final NotificacaoService notificacaoService;
    private final UsuarioRepository usuarioRepository;

    public NotificacaoScheduler(EstadiaRepository estadiaRepository, NotificacaoRepository notificacaoRepository,
                                 NotificacaoService notificacaoService, UsuarioRepository usuarioRepository) {
        this.estadiaRepository = estadiaRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.notificacaoService = notificacaoService;
        this.usuarioRepository = usuarioRepository;
    }

    @Scheduled(fixedRate = 300000)
    public void verificarReservasPendentes() {
        LocalDateTime agora = LocalDateTime.now();

        for (Estadia estadia : estadiaRepository.findByPendenteTrueAndPrevisaoChegadaIsNotNull()) {
            long minutosParaPrevisao = ChronoUnit.MINUTES.between(agora, estadia.getPrevisaoChegada());
            long minutosAtraso = ChronoUnit.MINUTES.between(estadia.getPrevisaoChegada(), agora);

            if (minutosParaPrevisao <= LIMITE_EXPIRANDO_MINUTOS && minutosAtraso <= LIMITE_ATRASO_MINUTOS) {
                notificarExpiracao(estadia);
            }

            if (minutosAtraso > LIMITE_ATRASO_MINUTOS) {
                notificarCheckinAtrasado(estadia);
            }
        }
    }

    private void notificarExpiracao(Estadia estadia) {
        if (notificacaoRepository.existsByEstadiaIdAndTipo(estadia.getId(), TipoNotificacao.RESERVA_EXPIRANDO)) {
            return;
        }
        notificacaoService.criar(estadia.getVeiculo().getUsuario(), TipoNotificacao.RESERVA_EXPIRANDO,
                "Reserva prestes a expirar",
                "Sua reserva na vaga " + estadia.getVaga().getCodigo() + " está prestes a expirar. Faça o check-in.",
                estadia.getId());
    }

    private void notificarCheckinAtrasado(Estadia estadia) {
        if (notificacaoRepository.existsByEstadiaIdAndTipo(estadia.getId(), TipoNotificacao.CHECKIN_ATRASADO)) {
            return;
        }
        Long estacionamentoId = estadia.getVaga().getEstacionamento().getId();
        List<Usuario> operadores = usuarioRepository.findByEstacionamentoIdAndRole(estacionamentoId, "OPERADOR");
        for (Usuario operador : operadores) {
            notificacaoService.criar(operador, TipoNotificacao.CHECKIN_ATRASADO,
                    "Check-in atrasado",
                    "O motorista da placa " + estadia.getVeiculo().getPlaca()
                            + " não fez check-in na vaga " + estadia.getVaga().getCodigo() + " na previsão informada.",
                    estadia.getId());
        }
    }
}
