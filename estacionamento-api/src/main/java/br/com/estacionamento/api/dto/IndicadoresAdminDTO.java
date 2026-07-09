package br.com.estacionamento.api.dto;

// Indicadores complementares do dashboard administrativo (fora das duas linhas de cards principais).
public class IndicadoresAdminDTO {

    private long usuariosOnline;
    private double crescimentoUsuariosPercentual;
    private double crescimentoFinanceiroPercentual;
    private String estacionamentoMaisMovimentado;
    private String estacionamentoMaisLucrativo;
    private long totalCheckinsHoje;
    private long totalCheckoutsHoje;
    private double mediaDiariaAcessos;
    private Integer picoUtilizacaoHora;
    private long totalPagamentosRealizados;
    private double receitaPrevistaMes;

    public IndicadoresAdminDTO(long usuariosOnline, double crescimentoUsuariosPercentual, double crescimentoFinanceiroPercentual,
                               String estacionamentoMaisMovimentado, String estacionamentoMaisLucrativo,
                               long totalCheckinsHoje, long totalCheckoutsHoje, double mediaDiariaAcessos,
                               Integer picoUtilizacaoHora, long totalPagamentosRealizados, double receitaPrevistaMes) {
        this.usuariosOnline = usuariosOnline;
        this.crescimentoUsuariosPercentual = crescimentoUsuariosPercentual;
        this.crescimentoFinanceiroPercentual = crescimentoFinanceiroPercentual;
        this.estacionamentoMaisMovimentado = estacionamentoMaisMovimentado;
        this.estacionamentoMaisLucrativo = estacionamentoMaisLucrativo;
        this.totalCheckinsHoje = totalCheckinsHoje;
        this.totalCheckoutsHoje = totalCheckoutsHoje;
        this.mediaDiariaAcessos = mediaDiariaAcessos;
        this.picoUtilizacaoHora = picoUtilizacaoHora;
        this.totalPagamentosRealizados = totalPagamentosRealizados;
        this.receitaPrevistaMes = receitaPrevistaMes;
    }

    public long getUsuariosOnline() { return usuariosOnline; }
    public double getCrescimentoUsuariosPercentual() { return crescimentoUsuariosPercentual; }
    public double getCrescimentoFinanceiroPercentual() { return crescimentoFinanceiroPercentual; }
    public String getEstacionamentoMaisMovimentado() { return estacionamentoMaisMovimentado; }
    public String getEstacionamentoMaisLucrativo() { return estacionamentoMaisLucrativo; }
    public long getTotalCheckinsHoje() { return totalCheckinsHoje; }
    public long getTotalCheckoutsHoje() { return totalCheckoutsHoje; }
    public double getMediaDiariaAcessos() { return mediaDiariaAcessos; }
    public Integer getPicoUtilizacaoHora() { return picoUtilizacaoHora; }
    public long getTotalPagamentosRealizados() { return totalPagamentosRealizados; }
    public double getReceitaPrevistaMes() { return receitaPrevistaMes; }
}
