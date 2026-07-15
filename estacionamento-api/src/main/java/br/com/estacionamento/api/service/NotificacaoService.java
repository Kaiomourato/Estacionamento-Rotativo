package br.com.estacionamento.api.service;

import br.com.estacionamento.api.dto.NotificacaoDTO;
import br.com.estacionamento.api.exception.RecursoNaoEncontradoException;
import br.com.estacionamento.api.model.Notificacao;
import br.com.estacionamento.api.model.TipoNotificacao;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.NotificacaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificacaoService {

    private final NotificacaoRepository repository;

    public NotificacaoService(NotificacaoRepository repository) {
        this.repository = repository;
    }

    public Notificacao criar(Usuario destinatario, TipoNotificacao tipo, String titulo, String mensagem, Long estadiaId) {
        Notificacao notificacao = new Notificacao();
        notificacao.setDestinatario(destinatario);
        notificacao.setTipo(tipo);
        notificacao.setTitulo(titulo);
        notificacao.setMensagem(mensagem);
        notificacao.setEstadiaId(estadiaId);
        return repository.save(notificacao);
    }

    public List<NotificacaoDTO> listar(String email) {
        return repository.findByDestinatarioEmailOrderByCriadoEmDesc(email).stream()
                .map(NotificacaoDTO::new)
                .toList();
    }

    public long contarNaoLidas(String email) {
        return repository.countByDestinatarioEmailAndLidaFalse(email);
    }

    public void marcarComoLida(String email, Long id) {
        Notificacao notificacao = repository.findByIdAndDestinatarioEmail(id, email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada"));
        notificacao.setLida(true);
        repository.save(notificacao);
    }

    public void marcarTodasComoLidas(String email) {
        List<Notificacao> naoLidas = repository.findByDestinatarioEmailAndLidaFalse(email);
        naoLidas.forEach(notificacao -> notificacao.setLida(true));
        repository.saveAll(naoLidas);
    }
}
