package br.com.estacionamento.api.dto;

import br.com.estacionamento.api.model.TipoVeiculo;

public class VagaRequestDTO {

    private String codigo;

    // Tipo de veículo aceito pela vaga. Nulo = aceita qualquer tipo de veículo.
    private TipoVeiculo tipoVeiculo;

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public TipoVeiculo getTipoVeiculo() { return tipoVeiculo; }
    public void setTipoVeiculo(TipoVeiculo tipoVeiculo) { this.tipoVeiculo = tipoVeiculo; }
}
