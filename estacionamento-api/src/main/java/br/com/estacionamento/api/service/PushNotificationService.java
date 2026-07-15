package br.com.estacionamento.api.service;

import br.com.estacionamento.api.exception.RecursoNaoEncontradoException;
import br.com.estacionamento.api.model.DispositivoFcm;
import br.com.estacionamento.api.model.TipoNotificacao;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.DispositivoFcmRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Dispara a notificação já criada no banco (ver NotificacaoService.criar) também como push
// real via Firebase Cloud Messaging, para chegar ao usuário mesmo com a aba/app fechados.
// FirebaseMessaging pode ser null (ver FirebaseConfig) quando a credencial não está configurada
// — nesse caso o envio é pulado silenciosamente, sem quebrar o fluxo de criação da notificação.
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final FirebaseMessaging firebaseMessaging;
    private final DispositivoFcmRepository dispositivoFcmRepository;
    private final UsuarioRepository usuarioRepository;

    public PushNotificationService(FirebaseMessaging firebaseMessaging, DispositivoFcmRepository dispositivoFcmRepository,
                                    UsuarioRepository usuarioRepository) {
        this.firebaseMessaging = firebaseMessaging;
        this.dispositivoFcmRepository = dispositivoFcmRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Registra (ou realoca, se o mesmo navegador logar como outro usuário depois) o token
    // de push deste dispositivo para o usuário autenticado.
    @Transactional
    public void registrarDispositivo(String email, String token) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        DispositivoFcm dispositivo = dispositivoFcmRepository.findByToken(token)
                .orElseGet(() -> new DispositivoFcm(usuario, token));
        dispositivo.setUsuario(usuario);
        dispositivo.setToken(token);
        dispositivoFcmRepository.save(dispositivo);
    }

    public void removerDispositivo(String token) {
        dispositivoFcmRepository.deleteByToken(token);
    }

    public void enviar(Usuario destinatario, TipoNotificacao tipo, String titulo, String mensagem, Long estadiaId) {
        if (firebaseMessaging == null) {
            return;
        }
        List<DispositivoFcm> dispositivos = dispositivoFcmRepository.findByUsuarioEmail(destinatario.getEmail());
        for (DispositivoFcm dispositivo : dispositivos) {
            enviarParaToken(dispositivo, tipo, titulo, mensagem, estadiaId);
        }
    }

    private void enviarParaToken(DispositivoFcm dispositivo, TipoNotificacao tipo, String titulo, String mensagem, Long estadiaId) {
        Message.Builder builder = Message.builder()
                .setToken(dispositivo.getToken())
                .setNotification(Notification.builder().setTitle(titulo).setBody(mensagem).build())
                .putData("tipo", tipo.name());
        if (estadiaId != null) {
            builder.putData("estadiaId", String.valueOf(estadiaId));
        }
        try {
            firebaseMessaging.send(builder.build());
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                // Token expirado/inválido (app desinstalado, permissão revogada, etc.) — remove
                // para não tentar de novo a cada notificação futura.
                dispositivoFcmRepository.deleteByToken(dispositivo.getToken());
            } else {
                log.warn("Falha ao enviar push para o dispositivo {}: {}", dispositivo.getId(), e.getMessage());
            }
        }
    }
}
