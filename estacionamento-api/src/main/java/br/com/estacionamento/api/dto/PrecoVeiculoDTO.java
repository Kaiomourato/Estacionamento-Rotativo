package br.com.estacionamento.api.dto;

import br.com.estacionamento.api.model.TipoVeiculo;

public class PrecoVeiculoDTO {

    private TipoVeiculo tipoVeiculo;
    private Double valorHora;

    public PrecoVeiculoDTO() {}

    public PrecoVeiculoDTO(TipoVeiculo tipoVeiculo, Double valorHora) {
        this.tipoVeiculo = tipoVeiculo;
        this.valorHora = valorHora;
    }

    public TipoVeiculo getTipoVeiculo() { return tipoVeiculo; }
    public void setTipoVeiculo(TipoVeiculo tipoVeiculo) { this.tipoVeiculo = tipoVeiculo; }

    public Double getValorHora() { return valorHora; }
    public void setValorHora(Double valorHora) { this.valorHora = valorHora; }
}
