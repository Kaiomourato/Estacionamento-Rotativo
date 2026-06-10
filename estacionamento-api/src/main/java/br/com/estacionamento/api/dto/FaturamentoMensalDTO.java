package br.com.estacionamento.api.dto;

public class FaturamentoMensalDTO {

    private String data;
    private double valor;

    public FaturamentoMensalDTO(String data, double valor) {
        this.data = data;
        this.valor = valor;
    }

    public String getData() { return data; }
    public double getValor() { return valor; }
}
