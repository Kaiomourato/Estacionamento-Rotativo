package br.com.estacionamento.api.dto;

import java.util.List;

public class DashboardAdminDTO {

    private CardsAdminDTO cards;
    private IndicadoresAdminDTO indicadores;
    // Faturamento (aberto/hoje/semana/mês/ano), fluxo de check-ins/check-outs dos últimos 7 dias
    // e faturamento dos últimos 12 meses — mesma estrutura usada no painel do operador, somada
    // entre todos os estacionamentos (ou recortada para um só, se filtrado).
    private RelatorioOperadorDTO financeiro;
    private List<SerieContagemDTO> checkinsPorMes;
    private List<SerieContagemDTO> horariosMovimento;
    private List<SerieContagemDTO> crescimentoUsuarios;
    private List<RankingEstacionamentoDTO> topEstacionamentos;

    public DashboardAdminDTO(CardsAdminDTO cards, IndicadoresAdminDTO indicadores, RelatorioOperadorDTO financeiro,
                              List<SerieContagemDTO> checkinsPorMes, List<SerieContagemDTO> horariosMovimento,
                              List<SerieContagemDTO> crescimentoUsuarios, List<RankingEstacionamentoDTO> topEstacionamentos) {
        this.cards = cards;
        this.indicadores = indicadores;
        this.financeiro = financeiro;
        this.checkinsPorMes = checkinsPorMes;
        this.horariosMovimento = horariosMovimento;
        this.crescimentoUsuarios = crescimentoUsuarios;
        this.topEstacionamentos = topEstacionamentos;
    }

    public CardsAdminDTO getCards() { return cards; }
    public IndicadoresAdminDTO getIndicadores() { return indicadores; }
    public RelatorioOperadorDTO getFinanceiro() { return financeiro; }
    public List<SerieContagemDTO> getCheckinsPorMes() { return checkinsPorMes; }
    public List<SerieContagemDTO> getHorariosMovimento() { return horariosMovimento; }
    public List<SerieContagemDTO> getCrescimentoUsuarios() { return crescimentoUsuarios; }
    public List<RankingEstacionamentoDTO> getTopEstacionamentos() { return topEstacionamentos; }
}
