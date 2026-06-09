package br.com.estacionamento.api.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@Entity
@Table(name = "estacionamentos")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Estacionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String endereco;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // Valor padrão da hora, usado para tipos de veículo sem preço específico cadastrado
    @Column(nullable = false)
    private Double valorHora;

    @JsonIgnore // <-- ADICIONADO PARA EVITAR LOOP INFINITO NO JSON
    @OneToMany(mappedBy = "estacionamento")
    private List<Vaga> vagas;

    @JsonIgnore
    @OneToMany(mappedBy = "estacionamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrecoVeiculo> precos;

    public Estacionamento() {}

    public Estacionamento(String nome, String endereco, Double latitude, Double longitude, Double valorHora) {
        this.nome = nome;
        this.endereco = endereco;
        this.latitude = latitude;
        this.longitude = longitude;
        this.valorHora = valorHora;
    }

    // Getters e Setters
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
    public Double getValorHora() { return valorHora; }
    public void setValorHora(Double valorHora) { this.valorHora = valorHora; }
    public List<Vaga> getVagas() { return vagas; }
    public void setVagas(List<Vaga> vagas) { this.vagas = vagas; }
    public List<PrecoVeiculo> getPrecos() { return precos; }
    public void setPrecos(List<PrecoVeiculo> precos) { this.precos = precos; }
}
