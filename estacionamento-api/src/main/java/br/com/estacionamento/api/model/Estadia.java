package br.com.estacionamento.api.model;

import jakarta.persistence.*;
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

    @Column(nullable = false)
    private LocalDateTime entrada;

    @Column
    private LocalDateTime saida;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column
    private Double valor;

    /** Código de check-in gerado na reserva — null para entradas manuais */
    @Column(unique = true)
    private String codigo;

    /** true = reserva aguardando check-in, false = já no pátio ou encerrada */
    @Column(nullable = false)
    private boolean pendente = false;

    public Estadia() {}

    public Long getId() { return id; }
    public Veiculo getVeiculo() { return veiculo; }
    public void setVeiculo(Veiculo veiculo) { this.veiculo = veiculo; }
    public Vaga getVaga() { return vaga; }
    public void setVaga(Vaga vaga) { this.vaga = vaga; }
    public LocalDateTime getEntrada() { return entrada; }
    public void setEntrada(LocalDateTime entrada) { this.entrada = entrada; }
    public LocalDateTime getSaida() { return saida; }
    public void setSaida(LocalDateTime saida) { this.saida = saida; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public boolean isPendente() { return pendente; }
    public void setPendente(boolean pendente) { this.pendente = pendente; }
}
