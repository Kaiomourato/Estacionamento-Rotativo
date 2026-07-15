package br.com.estacionamento.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// Token de push (Firebase Cloud Messaging) de um dispositivo/navegador do usuário.
// Um usuário pode ter vários (login em mais de um navegador/aparelho); o token é
// único porque o mesmo dispositivo não deve gerar duas linhas ao registrar de novo.
@Entity
@Table(name = "dispositivos_fcm")
public class DispositivoFcm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    @JsonIgnore
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 4096)
    private String token;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    public DispositivoFcm() {
    }

    public DispositivoFcm(Usuario usuario, String token) {
        this.usuario = usuario;
        this.token = token;
    }

    @PrePersist
    private void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
