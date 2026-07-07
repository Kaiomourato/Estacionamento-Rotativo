package br.com.estacionamento.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "logs_acesso")
public class LogAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String usuarioEmail;

    @Column(nullable = false)
    private String metodo;

    @Column(nullable = false)
    private String rota;

    @Column
    private Integer status;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    public LogAcesso() {}

    public LogAcesso(String usuarioEmail, String metodo, String rota, Integer status, LocalDateTime dataHora) {
        this.usuarioEmail = usuarioEmail;
        this.metodo = metodo;
        this.rota = rota;
        this.status = status;
        this.dataHora = dataHora;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public String getRota() { return rota; }
    public void setRota(String rota) { this.rota = rota; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
