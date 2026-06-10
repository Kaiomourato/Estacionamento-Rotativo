package br.com.estacionamento.api.dto;

public class FluxoDiaDTO {

    private String data;
    private long entradas;
    private long saidas;

    public FluxoDiaDTO(String data, long entradas, long saidas) {
        this.data = data;
        this.entradas = entradas;
        this.saidas = saidas;
    }

    public String getData() { return data; }
    public long getEntradas() { return entradas; }
    public long getSaidas() { return saidas; }
}
