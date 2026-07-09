package br.com.estacionamento.api.dto;

// Linha 1 (visão geral) + linha 2 (financeiro) do dashboard administrativo.
public class CardsAdminDTO {

    // Linha 1 — visão geral
    private long totalUsuarios;
    private long totalOperadores;
    private long totalAdministradores;
    private long totalEstacionamentos;
    private long totalVagas;
    private double taxaMediaOcupacao;

    // Linha 2 — financeiro
    private double receitaHoje;
    private double receitaSemana;
    private double receitaMes;
    private double receitaAno;
    private double ticketMedio;
    private Double tempoMedioEstadiaMinutos;

    public CardsAdminDTO(long totalUsuarios, long totalOperadores, long totalAdministradores,
                          long totalEstacionamentos, long totalVagas, double taxaMediaOcupacao,
                          double receitaHoje, double receitaSemana, double receitaMes, double receitaAno,
                          double ticketMedio, Double tempoMedioEstadiaMinutos) {
        this.totalUsuarios = totalUsuarios;
        this.totalOperadores = totalOperadores;
        this.totalAdministradores = totalAdministradores;
        this.totalEstacionamentos = totalEstacionamentos;
        this.totalVagas = totalVagas;
        this.taxaMediaOcupacao = taxaMediaOcupacao;
        this.receitaHoje = receitaHoje;
        this.receitaSemana = receitaSemana;
        this.receitaMes = receitaMes;
        this.receitaAno = receitaAno;
        this.ticketMedio = ticketMedio;
        this.tempoMedioEstadiaMinutos = tempoMedioEstadiaMinutos;
    }

    public long getTotalUsuarios() { return totalUsuarios; }
    public long getTotalOperadores() { return totalOperadores; }
    public long getTotalAdministradores() { return totalAdministradores; }
    public long getTotalEstacionamentos() { return totalEstacionamentos; }
    public long getTotalVagas() { return totalVagas; }
    public double getTaxaMediaOcupacao() { return taxaMediaOcupacao; }
    public double getReceitaHoje() { return receitaHoje; }
    public double getReceitaSemana() { return receitaSemana; }
    public double getReceitaMes() { return receitaMes; }
    public double getReceitaAno() { return receitaAno; }
    public double getTicketMedio() { return ticketMedio; }
    public Double getTempoMedioEstadiaMinutos() { return tempoMedioEstadiaMinutos; }
}
