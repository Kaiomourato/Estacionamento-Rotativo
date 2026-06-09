package br.com.estacionamento.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vagas")
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVeiculo tipo = TipoVeiculo.CARRO;

    /**
     * Capacidade em "slots de moto":
     *   MOTO        = 1 slot
     *   CARRO       = 2 slots  (aceita 1 carro OU 2 motos)
     *   CAMINHONETE = 3 slots  (aceita 1 caminhonete OU 3 motos)
     * slotsUsados é a soma dos veículos ativos na vaga agora.
     */
    @Column(nullable = false)
    private int slotsTotal;

    @Column(nullable = false)
    private int slotsUsados = 0;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne
    @JoinColumn(name = "estacionamento_id")
    private Estacionamento estacionamento;

    public Vaga() {}

    // Helpers
    public boolean isOcupada() {
        return slotsUsados >= slotsTotal;
    }

    public boolean aceitaVeiculo(TipoVeiculo tipoVeiculo) {
        int slotsNecessarios = slotsDoTipo(tipoVeiculo);
        // Vaga de moto não aceita carro nem caminhonete
        if (this.tipo == TipoVeiculo.MOTO && tipoVeiculo != TipoVeiculo.MOTO) return false;
        // Vaga de carro não aceita caminhonete
        if (this.tipo == TipoVeiculo.CARRO && tipoVeiculo == TipoVeiculo.CAMINHONETE) return false;
        return (slotsUsados + slotsNecessarios) <= slotsTotal;
    }

    public void ocupar(TipoVeiculo tipoVeiculo) {
        this.slotsUsados += slotsDoTipo(tipoVeiculo);
    }

    public void liberar(TipoVeiculo tipoVeiculo) {
        this.slotsUsados = Math.max(0, this.slotsUsados - slotsDoTipo(tipoVeiculo));
    }

    public static int slotsDoTipo(TipoVeiculo tipo) {
        return switch (tipo) {
            case MOTO        -> 1;
            case CARRO       -> 2;
            case CAMINHONETE -> 3;
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public TipoVeiculo getTipo() { return tipo; }
    public void setTipo(TipoVeiculo tipo) {
        this.tipo = tipo;
        this.slotsTotal = slotsDoTipo(tipo);
    }

    public int getSlotsTotal() { return slotsTotal; }
    public void setSlotsTotal(int slotsTotal) { this.slotsTotal = slotsTotal; }

    public int getSlotsUsados() { return slotsUsados; }
    public void setSlotsUsados(int slotsUsados) { this.slotsUsados = slotsUsados; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public Estacionamento getEstacionamento() { return estacionamento; }
    public void setEstacionamento(Estacionamento estacionamento) { this.estacionamento = estacionamento; }
}
