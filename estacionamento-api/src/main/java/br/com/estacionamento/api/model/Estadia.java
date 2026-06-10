package br.com.estacionamento.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import java.time.LocalDateTime;

@Entity
@Table(name = "estadias")
public class Estadia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vaga_id")
    private Vaga vaga;

    @Column
    private LocalDateTime entrada;

    @Column
    private LocalDateTime saida;

    @Column(nullable = false)
    private boolean ativa = true;

    // Reserva feita pelo motorista, aguardando check-in do operador
    @Column(nullable = false)
    private boolean pendente = false;

    // Código de check-in gerado quando a vaga é reservada
    @Column(length = 6)
    private String codigo;

    @Column
    private Double valor;

    // Momento em que o registro foi criado (preenchido automaticamente pelo backend)
    @Column
    private LocalDateTime criadoEm;

    // Horário em que o motorista espera chegar para fazer o check-in (opcional)
    @Column
    private LocalDateTime previsaoChegada;

    // Reserva cancelada pelo operador antes do check-in
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean cancelada = false;

    // Controla se o motorista já foi avisado do cancelamento via /minha-ativa
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean notificacaoCancelamentoLida = false;


    public Estadia() {}

   
    public Long getId() {
        return id;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }

    public Vaga getVaga() {
        return vaga;
    }

    public void setVaga(Vaga vaga) {
        this.vaga = vaga;
    }

    public LocalDateTime getEntrada() {
        return entrada;
    }

    public void setEntrada(LocalDateTime entrada) {
        this.entrada = entrada;
    }

    public LocalDateTime getSaida() {
        return saida;
    }

    public void setSaida(LocalDateTime saida) {
        this.saida = saida;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public boolean isPendente() {
        return pendente;
    }

    public void setPendente(boolean pendente) {
        this.pendente = pendente;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getPrevisaoChegada() {
        return previsaoChegada;
    }

    public void setPrevisaoChegada(LocalDateTime previsaoChegada) {
        this.previsaoChegada = previsaoChegada;
    }

    public boolean isCancelada() {
        return cancelada;
    }

    public void setCancelada(boolean cancelada) {
        this.cancelada = cancelada;
    }

    @JsonIgnore
    public boolean isNotificacaoCancelamentoLida() {
        return notificacaoCancelamentoLida;
    }

    public void setNotificacaoCancelamentoLida(boolean notificacaoCancelamentoLida) {
        this.notificacaoCancelamentoLida = notificacaoCancelamentoLida;
    }

    // Status derivado, exposto no JSON para o frontend (PENDENTE | ATIVA | FINALIZADA | CANCELADA)
    public StatusEstadia getStatus() {
        if (cancelada) return StatusEstadia.CANCELADA;
        if (pendente) return StatusEstadia.PENDENTE;
        if (!ativa) return StatusEstadia.FINALIZADA;
        return StatusEstadia.ATIVA;
    }
}
