package br.com.estacionamento.api.dto;

import java.util.List;

public class RelatorioOperadorDTO {

    private FaturamentoDTO faturamento;
    private List<FluxoDiaDTO> fluxoSemanal;
    private List<FaturamentoMensalDTO> faturamentoMensal;

    public RelatorioOperadorDTO(FaturamentoDTO faturamento, List<FluxoDiaDTO> fluxoSemanal, List<FaturamentoMensalDTO> faturamentoMensal) {
        this.faturamento = faturamento;
        this.fluxoSemanal = fluxoSemanal;
        this.faturamentoMensal = faturamentoMensal;
    }

    public FaturamentoDTO getFaturamento() { return faturamento; }
    public List<FluxoDiaDTO> getFluxoSemanal() { return fluxoSemanal; }
    public List<FaturamentoMensalDTO> getFaturamentoMensal() { return faturamentoMensal; }
}
