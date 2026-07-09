package br.com.estacionamento.api.dto;

public class CardsAdminDTO {

    private long totalUsuarios;
    private long totalAdministradores;
    private long totalOperadores;
    private long totalMotoristas;
    private long totalEstacionamentos;
    private long estacionamentosAtivos;
    private long estacionamentosInativos;
    private long totalVagas;
    private long vagasLivres;
    private long vagasOcupadas;
    private long totalCheckins;
    private long totalCheckouts;
    private double faturamentoTotal;
    private Double tempoMedioPermanenciaMinutos;
    private long totalAcessos;
    private long totalLogins;
    private long usuariosAtivos24h;
    private Integer picoUtilizacaoHora;
    private double mediaDiariaUtilizacao;

    public CardsAdminDTO(long totalUsuarios, long totalAdministradores, long totalOperadores, long totalMotoristas,
                          long totalEstacionamentos, long estacionamentosAtivos, long estacionamentosInativos,
                          long totalVagas, long vagasLivres, long vagasOcupadas,
                          long totalCheckins, long totalCheckouts, double faturamentoTotal, Double tempoMedioPermanenciaMinutos,
                          long totalAcessos, long totalLogins, long usuariosAtivos24h,
                          Integer picoUtilizacaoHora, double mediaDiariaUtilizacao) {
        this.totalUsuarios = totalUsuarios;
        this.totalAdministradores = totalAdministradores;
        this.totalOperadores = totalOperadores;
        this.totalMotoristas = totalMotoristas;
        this.totalEstacionamentos = totalEstacionamentos;
        this.estacionamentosAtivos = estacionamentosAtivos;
        this.estacionamentosInativos = estacionamentosInativos;
        this.totalVagas = totalVagas;
        this.vagasLivres = vagasLivres;
        this.vagasOcupadas = vagasOcupadas;
        this.totalCheckins = totalCheckins;
        this.totalCheckouts = totalCheckouts;
        this.faturamentoTotal = faturamentoTotal;
        this.tempoMedioPermanenciaMinutos = tempoMedioPermanenciaMinutos;
        this.totalAcessos = totalAcessos;
        this.totalLogins = totalLogins;
        this.usuariosAtivos24h = usuariosAtivos24h;
        this.picoUtilizacaoHora = picoUtilizacaoHora;
        this.mediaDiariaUtilizacao = mediaDiariaUtilizacao;
    }

    public long getTotalUsuarios() { return totalUsuarios; }
    public long getTotalAdministradores() { return totalAdministradores; }
    public long getTotalOperadores() { return totalOperadores; }
    public long getTotalMotoristas() { return totalMotoristas; }
    public long getTotalEstacionamentos() { return totalEstacionamentos; }
    public long getEstacionamentosAtivos() { return estacionamentosAtivos; }
    public long getEstacionamentosInativos() { return estacionamentosInativos; }
    public long getTotalVagas() { return totalVagas; }
    public long getVagasLivres() { return vagasLivres; }
    public long getVagasOcupadas() { return vagasOcupadas; }
    public long getTotalCheckins() { return totalCheckins; }
    public long getTotalCheckouts() { return totalCheckouts; }
    public double getFaturamentoTotal() { return faturamentoTotal; }
    public Double getTempoMedioPermanenciaMinutos() { return tempoMedioPermanenciaMinutos; }
    public long getTotalAcessos() { return totalAcessos; }
    public long getTotalLogins() { return totalLogins; }
    public long getUsuariosAtivos24h() { return usuariosAtivos24h; }
    public Integer getPicoUtilizacaoHora() { return picoUtilizacaoHora; }
    public double getMediaDiariaUtilizacao() { return mediaDiariaUtilizacao; }
}
