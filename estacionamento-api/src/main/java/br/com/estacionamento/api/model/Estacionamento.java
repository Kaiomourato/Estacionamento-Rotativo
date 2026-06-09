package br.com.estacionamento.api.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Entity
@Table(name = "estacionamentos")
public class Estacionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String endereco;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(nullable = false)
    private Integer vagasTotais;

    // Preços por tipo de veículo (nullable = usa valorHoraDefault se não definido)
    @Column
    private Double valorHoraCarro;

    @Column
    private Double valorHoraMoto;

    @Column
    private Double valorHoraCaminhonete;

    // Mantido por retrocompatibilidade — usado como fallback
    @Column(nullable = false)
    private Double valorHora;

    @JsonIgnore
    @OneToMany(mappedBy = "estacionamento")
    private List<Vaga> vagas;

    public Estacionamento() {}

    /** Retorna o valor/hora para um tipo específico, com fallback para valorHora genérico */
    public Double getValorHoraPorTipo(TipoVeiculo tipo) {
        return switch (tipo) {
            case CARRO       -> valorHoraCarro       != null ? valorHoraCarro       : valorHora;
            case MOTO        -> valorHoraMoto        != null ? valorHoraMoto        : valorHora;
            case CAMINHONETE -> valorHoraCaminhonete != null ? valorHoraCaminhonete : valorHora;
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Integer getVagasTotais() { return vagasTotais; }
    public void setVagasTotais(Integer vagasTotais) { this.vagasTotais = vagasTotais; }
    public Double getValorHora() { return valorHora; }
    public void setValorHora(Double valorHora) { this.valorHora = valorHora; }
    public Double getValorHoraCarro() { return valorHoraCarro; }
    public void setValorHoraCarro(Double v) { this.valorHoraCarro = v; }
    public Double getValorHoraMoto() { return valorHoraMoto; }
    public void setValorHoraMoto(Double v) { this.valorHoraMoto = v; }
    public Double getValorHoraCaminhonete() { return valorHoraCaminhonete; }
    public void setValorHoraCaminhonete(Double v) { this.valorHoraCaminhonete = v; }
    public List<Vaga> getVagas() { return vagas; }
    public void setVagas(List<Vaga> vagas) { this.vagas = vagas; }
}
