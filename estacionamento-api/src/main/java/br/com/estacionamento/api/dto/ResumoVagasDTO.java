package br.com.estacionamento.api.dto;

import br.com.estacionamento.api.model.TipoVeiculo;

public class ResumoVagasDTO {

    // Nulo representa as vagas que aceitam qualquer tipo de veículo
    private TipoVeiculo tipoVeiculo;
    private long total;
    private long livres;
    private long ocupadas;

    public ResumoVagasDTO(TipoVeiculo tipoVeiculo, long total, long livres, long ocupadas) {
        this.tipoVeiculo = tipoVeiculo;
        this.total = total;
        this.livres = livres;
        this.ocupadas = ocupadas;
    }

    public TipoVeiculo getTipoVeiculo() { return tipoVeiculo; }
    public long getTotal() { return total; }
    public long getLivres() { return livres; }
    public long getOcupadas() { return ocupadas; }
}
