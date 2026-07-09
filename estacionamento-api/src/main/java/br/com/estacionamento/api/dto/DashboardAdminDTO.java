package br.com.estacionamento.api.dto;

import java.util.List;

public class DashboardAdminDTO {

    private CardsAdminDTO cards;
    // Faturamento (aberto/semana/mês/ano), fluxo de check-ins/check-outs dos últimos 7 dias
    // e faturamento dos últimos 12 meses — mesma estrutura usada no painel do operador, somada
    // entre todos os estacionamentos.
    private RelatorioOperadorDTO financeiro;
    private List<SerieContagemDTO> checkinsPorMes;
    private List<SerieContagemDTO> horariosMovimento;
    private List<SerieContagemDTO> crescimentoUsuarios;
    private List<RankingEstacionamentoDTO> ocupacaoEstacionamentos;

    public DashboardAdminDTO(CardsAdminDTO cards, RelatorioOperadorDTO financeiro, List<SerieContagemDTO> checkinsPorMes,
                              List<SerieContagemDTO> horariosMovimento, List<SerieContagemDTO> crescimentoUsuarios,
                              List<RankingEstacionamentoDTO> ocupacaoEstacionamentos) {
        this.cards = cards;
        this.financeiro = financeiro;
        this.checkinsPorMes = checkinsPorMes;
        this.horariosMovimento = horariosMovimento;
        this.crescimentoUsuarios = crescimentoUsuarios;
        this.ocupacaoEstacionamentos = ocupacaoEstacionamentos;
    }

    public CardsAdminDTO getCards() { return cards; }
    public RelatorioOperadorDTO getFinanceiro() { return financeiro; }
    public List<SerieContagemDTO> getCheckinsPorMes() { return checkinsPorMes; }
    public List<SerieContagemDTO> getHorariosMovimento() { return horariosMovimento; }
    public List<SerieContagemDTO> getCrescimentoUsuarios() { return crescimentoUsuarios; }
    public List<RankingEstacionamentoDTO> getOcupacaoEstacionamentos() { return ocupacaoEstacionamentos; }
}
