package br.com.estacionamento.api.service;

import br.com.estacionamento.api.dto.CardsAdminDTO;
import br.com.estacionamento.api.dto.DashboardAdminDTO;
import br.com.estacionamento.api.dto.RankingEstacionamentoDTO;
import br.com.estacionamento.api.dto.RelatorioOperadorDTO;
import br.com.estacionamento.api.dto.SerieContagemDTO;
import br.com.estacionamento.api.dto.UsuarioResumoDTO;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.model.LogAcesso;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.EstadiaRepository;
import br.com.estacionamento.api.repository.LogAcessoRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Orquestra o dashboard do painel ADM: combina os repositórios existentes e a
// agregação financeira já usada no painel do operador (EstadiaService.gerarRelatorioGlobal),
// sem duplicar nenhuma lógica de cálculo.
@Service
public class AdminDashboardService {

    private static final String[] MESES_PT = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private final UsuarioRepository usuarioRepository;
    private final EstacionamentoRepository estacionamentoRepository;
    private final EstadiaRepository estadiaRepository;
    private final LogAcessoRepository logAcessoRepository;
    private final EstadiaService estadiaService;

    public AdminDashboardService(UsuarioRepository usuarioRepository, EstacionamentoRepository estacionamentoRepository,
                                  EstadiaRepository estadiaRepository, LogAcessoRepository logAcessoRepository,
                                  EstadiaService estadiaService) {
        this.usuarioRepository = usuarioRepository;
        this.estacionamentoRepository = estacionamentoRepository;
        this.estadiaRepository = estadiaRepository;
        this.logAcessoRepository = logAcessoRepository;
        this.estadiaService = estadiaService;
    }

    public DashboardAdminDTO montarDashboard(LocalDate inicio, LocalDate fim, Long estacionamentoId) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioEfetivo = inicio != null ? inicio : hoje.minusDays(29);
        LocalDate fimEfetivo = fim != null ? fim : hoje;
        LocalDateTime desde = inicioEfetivo.atStartOfDay();
        LocalDateTime ate = fimEfetivo.plusDays(1).atStartOfDay();
        long dias = Math.max(1, ChronoUnit.DAYS.between(inicioEfetivo, fimEfetivo) + 1);

        List<Estacionamento> estacionamentos = estacionamentoRepository.findAll();

        CardsAdminDTO cards = montarCards(desde, ate, dias, estacionamentoId, estacionamentos);
        RelatorioOperadorDTO financeiro = estadiaService.gerarRelatorioGlobal();
        List<SerieContagemDTO> checkinsPorMes = montarCheckinsPorMes(estacionamentoId);
        List<SerieContagemDTO> horariosMovimento = montarHorariosMovimento(desde, ate, estacionamentoId);
        List<SerieContagemDTO> crescimentoUsuarios = montarCrescimentoUsuarios();
        List<RankingEstacionamentoDTO> ocupacaoEstacionamentos = montarRankingEstacionamentos(estacionamentos);

        return new DashboardAdminDTO(cards, financeiro, checkinsPorMes, horariosMovimento, crescimentoUsuarios, ocupacaoEstacionamentos);
    }

    private CardsAdminDTO montarCards(LocalDateTime desde, LocalDateTime ate, long dias, Long estacionamentoId,
                                       List<Estacionamento> estacionamentos) {
        long totalUsuarios = usuarioRepository.count();
        long totalAdministradores = usuarioRepository.countByRole("ADMIN");
        long totalOperadores = usuarioRepository.countByRoleAndEstacionamentoIsNotNull("USER");
        long totalMotoristas = usuarioRepository.countByRoleAndEstacionamentoIsNull("USER");
        long totalEstacionamentos = estacionamentos.size();

        // "Ativo" = tem ao menos uma vaga ativa (sem coluna dedicada no schema)
        long estacionamentosAtivos = estacionamentos.stream().filter(e -> e.getVagasTotais() > 0).count();
        long estacionamentosInativos = totalEstacionamentos - estacionamentosAtivos;

        long totalVagas = estacionamentos.stream().mapToLong(Estacionamento::getVagasTotais).sum();
        long vagasOcupadas = estacionamentos.stream().mapToLong(Estacionamento::getVagasOcupadas).sum();
        long vagasLivres = totalVagas - vagasOcupadas;

        List<Estadia> checkins = buscarEntradasDesde(estacionamentoId, desde).stream()
                .filter(e -> e.getEntrada().isBefore(ate)).collect(Collectors.toList());
        List<Estadia> checkouts = buscarSaidasDesde(estacionamentoId, desde).stream()
                .filter(e -> e.getSaida().isBefore(ate)).collect(Collectors.toList());

        long totalCheckins = checkins.size();
        long totalCheckouts = checkouts.size();
        double faturamentoTotal = arredondar(checkouts.stream().mapToDouble(e -> e.getValor() != null ? e.getValor() : 0.0).sum());

        List<Estadia> comDuracao = checkouts.stream()
                .filter(e -> e.getEntrada() != null && e.getSaida() != null)
                .collect(Collectors.toList());
        Double tempoMedioPermanenciaMinutos = comDuracao.isEmpty() ? null : arredondar(
                comDuracao.stream().mapToLong(e -> Duration.between(e.getEntrada(), e.getSaida()).toMinutes()).average().orElse(0));

        List<LogAcesso> logsPeriodo = logAcessoRepository.findByDataHoraGreaterThanEqual(desde).stream()
                .filter(l -> l.getDataHora().isBefore(ate)).collect(Collectors.toList());
        long totalAcessos = logsPeriodo.size();
        long totalLogins = logsPeriodo.stream()
                .filter(l -> l.getRota() != null && l.getRota().contains("/auth/login") && Integer.valueOf(200).equals(l.getStatus()))
                .count();
        long usuariosAtivos24h = logAcessoRepository.countUsuariosAtivosDesde(LocalDateTime.now().minusHours(24));

        Integer picoUtilizacaoHora = null;
        if (!logsPeriodo.isEmpty()) {
            long[] porHora = new long[24];
            for (LogAcesso l : logsPeriodo) porHora[l.getDataHora().getHour()]++;
            int hPico = 0;
            for (int h = 1; h < 24; h++) if (porHora[h] > porHora[hPico]) hPico = h;
            picoUtilizacaoHora = hPico;
        }
        double mediaDiariaUtilizacao = arredondar((double) totalAcessos / dias);

        return new CardsAdminDTO(totalUsuarios, totalAdministradores, totalOperadores, totalMotoristas,
                totalEstacionamentos, estacionamentosAtivos, estacionamentosInativos,
                totalVagas, vagasLivres, vagasOcupadas,
                totalCheckins, totalCheckouts, faturamentoTotal, tempoMedioPermanenciaMinutos,
                totalAcessos, totalLogins, usuariosAtivos24h, picoUtilizacaoHora, mediaDiariaUtilizacao);
    }

    private List<Estadia> buscarEntradasDesde(Long estacionamentoId, LocalDateTime desde) {
        return estacionamentoId != null
                ? estadiaRepository.findByVagaEstacionamentoIdAndEntradaIsNotNullAndEntradaGreaterThanEqual(estacionamentoId, desde)
                : estadiaRepository.findByEntradaIsNotNullAndEntradaGreaterThanEqual(desde);
    }

    private List<Estadia> buscarSaidasDesde(Long estacionamentoId, LocalDateTime desde) {
        return estacionamentoId != null
                ? estadiaRepository.findByVagaEstacionamentoIdAndSaidaIsNotNullAndSaidaGreaterThanEqual(estacionamentoId, desde)
                : estadiaRepository.findBySaidaIsNotNullAndSaidaGreaterThanEqual(desde);
    }

    private List<SerieContagemDTO> montarCheckinsPorMes(Long estacionamentoId) {
        LocalDateTime inicio12Meses = YearMonth.from(LocalDate.now()).minusMonths(11).atDay(1).atStartOfDay();
        List<Estadia> entradas = buscarEntradasDesde(estacionamentoId, inicio12Meses);

        List<SerieContagemDTO> resultado = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth mesAlvo = YearMonth.from(LocalDate.now()).minusMonths(i);
            long total = entradas.stream().filter(e -> YearMonth.from(e.getEntrada().toLocalDate()).equals(mesAlvo)).count();
            resultado.add(new SerieContagemDTO(rotuloMes(mesAlvo), total));
        }
        return resultado;
    }

    private List<SerieContagemDTO> montarHorariosMovimento(LocalDateTime desde, LocalDateTime ate, Long estacionamentoId) {
        List<Estadia> entradas = buscarEntradasDesde(estacionamentoId, desde).stream()
                .filter(e -> e.getEntrada().isBefore(ate)).collect(Collectors.toList());

        long[] porHora = new long[24];
        for (Estadia e : entradas) porHora[e.getEntrada().getHour()]++;

        return IntStream.range(0, 24)
                .mapToObj(h -> new SerieContagemDTO(String.format("%02dh", h), porHora[h]))
                .collect(Collectors.toList());
    }

    private List<SerieContagemDTO> montarCrescimentoUsuarios() {
        List<Usuario> usuariosComData = usuarioRepository.findByCriadoEmIsNotNullOrderByCriadoEmAsc();

        List<SerieContagemDTO> resultado = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth mesAlvo = YearMonth.from(LocalDate.now()).minusMonths(i);
            long total = usuariosComData.stream()
                    .filter(u -> YearMonth.from(u.getCriadoEm().toLocalDate()).equals(mesAlvo))
                    .count();
            resultado.add(new SerieContagemDTO(rotuloMes(mesAlvo), total));
        }
        return resultado;
    }

    private List<RankingEstacionamentoDTO> montarRankingEstacionamentos(List<Estacionamento> estacionamentos) {
        Map<Long, Long> contagemPorEstacionamento = new HashMap<>();
        for (Object[] linha : estadiaRepository.contarEstadiasPorEstacionamento()) {
            contagemPorEstacionamento.put((Long) linha[0], (Long) linha[1]);
        }

        return estacionamentos.stream()
                .map(e -> {
                    long totalEstadias = contagemPorEstacionamento.getOrDefault(e.getId(), 0L);
                    long vagasTotais = e.getVagasTotais();
                    long vagasOcupadas = e.getVagasOcupadas();
                    long vagasLivres = e.getVagasLivres();
                    double ocupacaoPercentual = vagasTotais > 0 ? arredondar(vagasOcupadas * 100.0 / vagasTotais) : 0.0;
                    return new RankingEstacionamentoDTO(e.getId(), e.getNome(), totalEstadias, vagasTotais, vagasOcupadas, vagasLivres, ocupacaoPercentual);
                })
                .sorted(Comparator.comparingLong(RankingEstacionamentoDTO::getTotalEstadias).reversed())
                .collect(Collectors.toList());
    }

    public List<UsuarioResumoDTO> listarUsuariosRecentes() {
        return usuarioRepository.findTop10ByOrderByIdDesc().stream()
                .map(u -> new UsuarioResumoDTO(u.getId(), u.getEmail(), u.getRole(), tipoUsuario(u),
                        u.getEstacionamento() != null ? u.getEstacionamento().getNome() : null, u.getCriadoEm()))
                .collect(Collectors.toList());
    }

    private String tipoUsuario(Usuario usuario) {
        if ("ADMIN".equalsIgnoreCase(usuario.getRole())) return "ADMIN";
        if (usuario.getEstacionamento() != null) return "OPERADOR";
        return "MOTORISTA";
    }

    public List<Estadia> listarUltimosCheckins(Long estacionamentoId, int limite) {
        PageRequest pagina = PageRequest.of(0, limite);
        return estacionamentoId != null
                ? estadiaRepository.findByVagaEstacionamentoIdAndEntradaIsNotNullOrderByEntradaDesc(estacionamentoId, pagina)
                : estadiaRepository.findByEntradaIsNotNullOrderByEntradaDesc(pagina);
    }

    public List<Estadia> listarUltimosCheckouts(Long estacionamentoId, int limite) {
        PageRequest pagina = PageRequest.of(0, limite);
        return estacionamentoId != null
                ? estadiaRepository.findByVagaEstacionamentoIdAndSaidaIsNotNullOrderBySaidaDesc(estacionamentoId, pagina)
                : estadiaRepository.findBySaidaIsNotNullOrderBySaidaDesc(pagina);
    }

    private String rotuloMes(YearMonth mes) {
        return MESES_PT[mes.getMonthValue() - 1] + "/" + String.format("%02d", mes.getYear() % 100);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
