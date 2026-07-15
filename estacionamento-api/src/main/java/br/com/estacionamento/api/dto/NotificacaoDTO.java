package br.com.estacionamento.api.dto;

import br.com.estacionamento.api.model.Notificacao;
import br.com.estacionamento.api.model.TipoNotificacao;

import java.time.LocalDateTime;

public class NotificacaoDTO {

    private Long id;
    private TipoNotificacao tipo;
    private String titulo;
    private String mensagem;
    private boolean lida;
    private LocalDateTime criadoEm;
    private Long estadiaId;

    public NotificacaoDTO(Notificacao notificacao) {
        this.id = notificacao.getId();
        this.tipo = notificacao.getTipo();
        this.titulo = notificacao.getTitulo();
        this.mensagem = notificacao.getMensagem();
        this.lida = notificacao.isLida();
        this.criadoEm = notificacao.getCriadoEm();
        this.estadiaId = notificacao.getEstadiaId();
    }

    public Long getId() { return id; }
    public TipoNotificacao getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getMensagem() { return mensagem; }
    public boolean isLida() { return lida; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public Long getEstadiaId() { return estadiaId; }
}
