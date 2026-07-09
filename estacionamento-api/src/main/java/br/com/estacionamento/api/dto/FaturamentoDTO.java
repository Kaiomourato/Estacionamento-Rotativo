package br.com.estacionamento.api.dto;

public class FaturamentoDTO {

    private double aberto;
    private double hoje;
    private double semana;
    private double mes;
    private double ano;

    public FaturamentoDTO(double aberto, double hoje, double semana, double mes, double ano) {
        this.aberto = aberto;
        this.hoje = hoje;
        this.semana = semana;
        this.mes = mes;
        this.ano = ano;
    }

    public double getAberto() { return aberto; }
    public double getHoje() { return hoje; }
    public double getSemana() { return semana; }
    public double getMes() { return mes; }
    public double getAno() { return ano; }
}
