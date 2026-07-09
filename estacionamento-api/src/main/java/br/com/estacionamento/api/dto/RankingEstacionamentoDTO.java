package br.com.estacionamento.api.dto;

public class RankingEstacionamentoDTO {

    private Long estacionamentoId;
    private String nome;
    private long totalEstadias;
    private long vagasTotais;
    private long vagasOcupadas;
    private long vagasLivres;
    private double ocupacaoPercentual;
    private double faturamentoTotal;

    public RankingEstacionamentoDTO(Long estacionamentoId, String nome, long totalEstadias,
                                     long vagasTotais, long vagasOcupadas, long vagasLivres,
                                     double ocupacaoPercentual, double faturamentoTotal) {
        this.estacionamentoId = estacionamentoId;
        this.nome = nome;
        this.totalEstadias = totalEstadias;
        this.vagasTotais = vagasTotais;
        this.vagasOcupadas = vagasOcupadas;
        this.vagasLivres = vagasLivres;
        this.ocupacaoPercentual = ocupacaoPercentual;
        this.faturamentoTotal = faturamentoTotal;
    }

    public Long getEstacionamentoId() { return estacionamentoId; }
    public String getNome() { return nome; }
    public long getTotalEstadias() { return totalEstadias; }
    public long getVagasTotais() { return vagasTotais; }
    public long getVagasOcupadas() { return vagasOcupadas; }
    public long getVagasLivres() { return vagasLivres; }
    public double getOcupacaoPercentual() { return ocupacaoPercentual; }
    public double getFaturamentoTotal() { return faturamentoTotal; }
}
