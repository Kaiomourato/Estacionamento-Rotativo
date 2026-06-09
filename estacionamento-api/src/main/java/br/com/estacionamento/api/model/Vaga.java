package br.com.estacionamento.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "vagas", uniqueConstraints = {
    // O código da vaga só precisa ser único DENTRO do mesmo estacionamento
    @UniqueConstraint(name = "uk_vaga_estacionamento_codigo", columnNames = {"estacionamento_id", "codigo"})
})
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private boolean ocupada;

    // NOVO: Soft Delete (Exclusão lógica para não quebrar as estadias antigas)
    @Column(nullable = false)
    private boolean ativo = true;

    // Tipo de veículo aceito por esta vaga. Nulo = aceita qualquer tipo de veículo.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_veiculo")
    private TipoVeiculo tipoVeiculo;

    @ManyToOne
    @JoinColumn(name = "estacionamento_id")
    @JsonIgnoreProperties({"vagas", "precos"})
    private Estacionamento estacionamento;

    public Vaga() {
    }

    public Vaga(String codigo, boolean ocupada) {
        this.codigo = codigo;
        this.ocupada = ocupada;
        this.ativo = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public boolean isOcupada() { return ocupada; }
    public void setOcupada(boolean ocupada) { this.ocupada = ocupada; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public TipoVeiculo getTipoVeiculo() { return tipoVeiculo; }
    public void setTipoVeiculo(TipoVeiculo tipoVeiculo) { this.tipoVeiculo = tipoVeiculo; }

    public Estacionamento getEstacionamento() { return estacionamento; }
    public void setEstacionamento(Estacionamento estacionamento) { this.estacionamento = estacionamento; }
}
