package br.com.estacionamento.api.service;

import br.com.estacionamento.api.dto.CardsAdminDTO;
import br.com.estacionamento.api.dto.DashboardAdminDTO;
import br.com.estacionamento.api.dto.FaturamentoMensalDTO;
import br.com.estacionamento.api.dto.IndicadoresAdminDTO;
import br.com.estacionamento.api.dto.RankingEstacionamentoDTO;
import br.com.estacionamento.api.dto.RelatorioOperadorDTO;
import br.com.estacionamento.api.dto.SerieContagemDTO;
import br.com.estacionamento.api.dto.UsuarioResumoDTO;
import br.com.estacionamento.api.exception.RecursoNaoEncontradoException;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.model.LogAcesso;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.EstadiaRepository;
import br.com.estacionamento.api.repository.LogAcessoRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import br.com.estacionamento.api.repository.VagaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Orquestra o dashboard do painel ADM e as listagens administrativas (usuários,
// vagas, pagamentos), combinando os repositórios existentes com a agregação
// financeira já usada no painel do operador (EstadiaService), sem duplicar lógica.
@Service
public class AdminDashboardService {

    private static final String[] MESES_PT = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private final UsuarioRepository usuarioRepository;
    private final EstacionamentoRepository estacionamentoRepository;
    private final EstadiaRepository estadiaRepository;
    private final VagaRepository vagaRepository;
    private final LogAcessoRepository logAcessoRepository;
    private final EstadiaService estadiaService;

    public AdminDashboardService(UsuarioRepository usuarioRepository, EstacionamentoRepository estacionamentoRepository,
                                  EstadiaRepository estadiaRepository, VagaRepository vagaRepository,
                                  LogAcessoRepository logAcessoRepository, EstadiaService estadiaService) {
        this.usuarioRepository = usuarioRepository;
        this.estacionamentoRepository = estacionamentoRepository;
        this.estadiaRepository = estadiaRepository;
        this.vagaRepository = vagaRepository;
        this.logAcessoRepository = logAcessoRepository;
        this.estadiaService = estadiaService;
    }

    public DashboardAdminDTO montarDashboard(Long estacionamentoId) {
        List<Estacionamento> estacionamentos = estacionamentoRepository.findAllComVagas();

        RelatorioOperadorDTO financeiro = estacionamentoId != null
                ? estadiaService.gerarRelatorioPorEstacionamento(buscarEstacionamento(estacionamentoId))
                : estadiaService.gerarRelatorioGlobal();

        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<Estadia> checkoutsMes = buscarSaidasDesde(estacionamentoId, inicioMes);

        List<SerieContagemDTO> checkinsPorMes = montarCheckinsPorMes(estacionamentoId);
        List<SerieContagemDTO> horariosMovimento = montarHorariosMovimento(estacionamentoId);
        List<SerieContagemDTO> crescimentoUsuarios = montarCrescimentoUsuarios();
        List<RankingEstacionamentoDTO> topEstacionamentos = montarTopEstacionamentos(estacionamentos);

        CardsAdminDTO cards = montarCards(estacionamentoId, estacionamentos, financeiro, checkoutsMes);
        IndicadoresAdminDTO indicadores = montarIndicadores(estacionamentoId, financeiro, crescimentoUsuarios, topEstacionamentos, checkoutsMes);

        return new DashboardAdminDTO(cards, indicadores, financeiro, checkinsPorMes, horariosMovimento, crescimentoUsuarios, topEstacionamentos);
    }

    private Estacionamento buscarEstacionamento(Long id) {
        return estacionamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estacionamento não encontrado"));
    }

    private CardsAdminDTO montarCards(Long estacionamentoId, List<Estacionamento> estacionamentos,
                                       RelatorioOperadorDTO financeiro, List<Estadia> checkoutsMes) {
        long totalUsuarios = usuarioRepository.count();
        long totalOperadores = usuarioRepository.countByRole("OPERADOR");
        long totalAdministradores = usuarioRepository.countByRole("ADMIN");
        long totalEstacionamentos = estacionamentos.size();

        List<Estacionamento> escopo = escopoEstacionamentos(estacionamentoId, estacionamentos);
        long totalVagas = escopo.stream().mapToLong(Estacionamento::getVagasTotais).sum();
        long vagasOcupadas = escopo.stream().mapToLong(Estacionamento::getVagasOcupadas).sum();
        double taxaMediaOcupacao = totalVagas > 0 ? arredondar(vagasOcupadas * 100.0 / totalVagas) : 0.0;

        long totalPagamentosMes = checkoutsMes.size();
        double receitaMesCalculada = checkoutsMes.stream().mapToDouble(e -> e.getValor() != null ? e.getValor() : 0.0).sum();
        double ticketMedio = totalPagamentosMes > 0 ? arredondar(receitaMesCalculada / totalPagamentosMes) : 0.0;

        List<Estadia> comDuracao = checkoutsMes.stream()
                .filter(e -> e.getEntrada() != null && e.getSaida() != null)
                .collect(Collectors.toList());
        Double tempoMedioEstadiaMinutos = comDuracao.isEmpty() ? null : arredondar(
                comDuracao.stream().mapToLong(e -> Duration.between(e.getEntrada(), e.getSaida()).toMinutes()).average().orElse(0));

        return new CardsAdminDTO(totalUsuarios, totalOperadores, totalAdministradores, totalEstacionamentos, totalVagas, taxaMediaOcupacao,
                financeiro.getFaturamento().getHoje(), financeiro.getFaturamento().getSemana(),
                financeiro.getFaturamento().getMes(), financeiro.getFaturamento().getAno(),
                ticketMedio, tempoMedioEstadiaMinutos);
    }

    private IndicadoresAdminDTO montarIndicadores(Long estacionamentoId, RelatorioOperadorDTO financeiro,
                                                   List<SerieContagemDTO> crescimentoUsuarios,
                                                   List<RankingEstacionamentoDTO> topEstacionamentos,
                                                   List<Estadia> checkoutsMes) {
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioHoje = hoje.atStartOfDay();

        long totalCheckinsHoje = buscarEntradasDesde(estacionamentoId, inicioHoje).size();
        long totalCheckoutsHoje = buscarSaidasDesde(estacionamentoId, inicioHoje).size();

        // "Online" = proxy apertado (5 min) via logs de acesso — não há WebSocket/heartbeat
        long usuariosOnline = logAcessoRepository.countUsuariosAtivosDesde(LocalDateTime.now().minusMinutes(5));

        LocalDateTime inicio30Dias = LocalDate.now().minusDays(29).atStartOfDay();
        List<LogAcesso> logs30Dias = logAcessoRepository.findByDataHoraGreaterThanEqual(inicio30Dias);
        double mediaDiariaAcessos = arredondar((double) logs30Dias.size() / 30);
        Integer picoUtilizacaoHora = null;
        if (!logs30Dias.isEmpty()) {
            long[] porHora = new long[24];
            for (LogAcesso l : logs30Dias) porHora[l.getDataHora().getHour()]++;
            int hPico = 0;
            for (int h = 1; h < 24; h++) if (porHora[h] > porHora[hPico]) hPico = h;
            picoUtilizacaoHora = hPico;
        }

        double crescimentoUsuariosPercentual = calcularCrescimentoPercentual(
                crescimentoUsuarios.size() >= 2 ? crescimentoUsuarios.get(crescimentoUsuarios.size() - 2).getQuantidade() : 0,
                crescimentoUsuarios.isEmpty() ? 0 : crescimentoUsuarios.get(crescimentoUsuarios.size() - 1).getQuantidade());

        List<FaturamentoMensalDTO> mensal = financeiro.getFaturamentoMensal();
        double receitaMesAtual = mensal.isEmpty() ? 0 : mensal.get(mensal.size() - 1).getValor();
        double receitaMesAnterior = mensal.size() >= 2 ? mensal.get(mensal.size() - 2).getValor() : 0;
        double crescimentoFinanceiroPercentual = calcularCrescimentoPercentual(receitaMesAnterior, receitaMesAtual);

        String estacionamentoMaisMovimentado = topEstacionamentos.isEmpty() ? null : topEstacionamentos.get(0).getNome();
        String estacionamentoMaisLucrativo = topEstacionamentos.stream()
                .max(Comparator.comparingDouble(RankingEstacionamentoDTO::getFaturamentoTotal))
                .map(RankingEstacionamentoDTO::getNome).orElse(null);

        int diaDoMes = hoje.getDayOfMonth();
        int diasNoMes = YearMonth.from(hoje).lengthOfMonth();
        double receitaPrevistaMes = diaDoMes > 0 ? arredondar((financeiro.getFaturamento().getMes() / diaDoMes) * diasNoMes) : 0.0;

        return new IndicadoresAdminDTO(usuariosOnline, crescimentoUsuariosPercentual, crescimentoFinanceiroPercentual,
                estacionamentoMaisMovimentado, estacionamentoMaisLucrativo, totalCheckinsHoje, totalCheckoutsHoje,
                mediaDiariaAcessos, picoUtilizacaoHora, checkoutsMes.size(), receitaPrevistaMes);
    }

    private double calcularCrescimentoPercentual(double anterior, double atual) {
        if (anterior <= 0) return atual > 0 ? 100.0 : 0.0;
        return arredondar(((atual - anterior) / anterior) * 100.0);
    }

    private List<Estacionamento> escopoEstacionamentos(Long estacionamentoId, List<Estacionamento> todos) {
        if (estacionamentoId == null) return todos;
        return todos.stream().filter(e -> e.getId().equals(estacionamentoId)).collect(Collectors.toList());
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

    // Últimos 30 dias — janela fixa (o dashboard não usa mais um filtro de período arbitrário)
    private List<SerieContagemDTO> montarHorariosMovimento(Long estacionamentoId) {
        LocalDateTime inicio30Dias = LocalDate.now().minusDays(29).atStartOfDay();
        List<Estadia> entradas = buscarEntradasDesde(estacionamentoId, inicio30Dias);

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

    private List<RankingEstacionamentoDTO> montarTopEstacionamentos(List<Estacionamento> estacionamentos) {
        Map<Long, Long> contagemPorEstacionamento = new HashMap<>();
        Map<Long, Double> faturamentoPorEstacionamento = new HashMap<>();
        for (Object[] linha : estadiaRepository.calcularEstatisticasPorEstacionamento()) {
            Long estacionamentoId = (Long) linha[0];
            contagemPorEstacionamento.put(estacionamentoId, (Long) linha[1]);
            faturamentoPorEstacionamento.put(estacionamentoId, (Double) linha[2]);
        }

        return estacionamentos.stream()
                .map(e -> {
                    long totalEstadias = contagemPorEstacionamento.getOrDefault(e.getId(), 0L);
                    double faturamentoTotal = faturamentoPorEstacionamento.getOrDefault(e.getId(), 0.0);
                    long vagasTotais = e.getVagasTotais();
                    long vagasOcupadas = e.getVagasOcupadas();
                    long vagasLivres = e.getVagasLivres();
                    double ocupacaoPercentual = vagasTotais > 0 ? arredondar(vagasOcupadas * 100.0 / vagasTotais) : 0.0;
                    return new RankingEstacionamentoDTO(e.getId(), e.getNome(), totalEstadias, vagasTotais, vagasOcupadas,
                            vagasLivres, ocupacaoPercentual, arredondar(faturamentoTotal));
                })
                .sorted(Comparator.comparingLong(RankingEstacionamentoDTO::getTotalEstadias).reversed())
                .collect(Collectors.toList());
    }

    public List<UsuarioResumoDTO> listarUsuariosRecentes() {
        return usuarioRepository.findTop10ByOrderByIdDesc().stream()
                .map(this::paraResumo)
                .collect(Collectors.toList());
    }

    // Listagem paginada e pesquisável — usada nas páginas Usuários e Operadores
    // (Operadores = mesma listagem com role fixado em "OPERADOR")
    public Page<UsuarioResumoDTO> listarUsuariosPaginado(String role, String busca, Pageable pageable) {
        return usuarioRepository.buscarParaAdmin(vazioParaNulo(role), vazioParaNulo(busca), pageable).map(this::paraResumo);
    }

    private UsuarioResumoDTO paraResumo(Usuario u) {
        return new UsuarioResumoDTO(u.getId(), u.getEmail(), u.getRole(), tipoUsuario(u),
                u.getEstacionamento() != null ? u.getEstacionamento().getNome() : null, u.getCriadoEm());
    }

    private String tipoUsuario(Usuario usuario) {
        if ("ADMIN".equalsIgnoreCase(usuario.getRole())) return "ADMIN";
        if ("OPERADOR".equalsIgnoreCase(usuario.getRole()) || usuario.getEstacionamento() != null) return "OPERADOR";
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

    // Página Vagas: todas as vagas de todos os estacionamentos, paginada e pesquisável
    public Page<Vaga> listarVagasPaginado(Long estacionamentoId, String busca, Pageable pageable) {
        return vagaRepository.buscarParaAdmin(estacionamentoId, vazioParaNulo(busca), pageable);
    }

    // Página Pagamentos: estadias finalizadas (com valor calculado no check-out)
    public Page<Estadia> listarPagamentosPaginado(Long estacionamentoId, LocalDate inicio, LocalDate fim, Pageable pageable) {
        LocalDateTime desde = inicio != null ? inicio.atStartOfDay() : null;
        LocalDateTime ate = fim != null ? fim.plusDays(1).atStartOfDay() : null;
        return estadiaRepository.buscarPagamentos(estacionamentoId, desde, ate, pageable);
    }

    private String vazioParaNulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor;
    }

    private String rotuloMes(YearMonth mes) {
        return MESES_PT[mes.getMonthValue() - 1] + "/" + String.format("%02d", mes.getYear() % 100);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
