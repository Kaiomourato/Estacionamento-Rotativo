package br.com.estacionamento.api.dto;

public class SerieContagemDTO {

    private String rotulo;
    private long quantidade;

    public SerieContagemDTO(String rotulo, long quantidade) {
        this.rotulo = rotulo;
        this.quantidade = quantidade;
    }

    public String getRotulo() { return rotulo; }
    public long getQuantidade() { return quantidade; }
}
