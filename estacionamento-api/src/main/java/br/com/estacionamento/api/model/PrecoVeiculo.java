package br.com.estacionamento.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "estacionamento_precos", uniqueConstraints = {
    @UniqueConstraint(name = "uk_preco_estacionamento_tipo", columnNames = {"estacionamento_id", "tipo_veiculo"})
})
public class PrecoVeiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "estacionamento_id")
    @JsonIgnoreProperties({"vagas", "precos", "vagasTotais", "vagasOcupadas", "vagasLivres"})
    private Estacionamento estacionamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_veiculo", nullable = false)
    private TipoVeiculo tipoVeiculo;

    @Column(nullable = false)
    private Double valorHora;

    public PrecoVeiculo() {}

    public PrecoVeiculo(Estacionamento estacionamento, TipoVeiculo tipoVeiculo, Double valorHora) {
        this.estacionamento = estacionamento;
        this.tipoVeiculo = tipoVeiculo;
        this.valorHora = valorHora;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Estacionamento getEstacionamento() { return estacionamento; }
    public void setEstacionamento(Estacionamento estacionamento) { this.estacionamento = estacionamento; }

    public TipoVeiculo getTipoVeiculo() { return tipoVeiculo; }
    public void setTipoVeiculo(TipoVeiculo tipoVeiculo) { this.tipoVeiculo = tipoVeiculo; }

    public Double getValorHora() { return valorHora; }
    public void setValorHora(Double valorHora) { this.valorHora = valorHora; }
}
