package br.com.estacionamento.api.service;

import br.com.estacionamento.api.dto.FaturamentoDTO;
import br.com.estacionamento.api.dto.FaturamentoMensalDTO;
import br.com.estacionamento.api.dto.FluxoDiaDTO;
import br.com.estacionamento.api.dto.RelatorioOperadorDTO;
import br.com.estacionamento.api.exception.RecursoNaoEncontradoException;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.model.TipoVeiculo;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.model.Veiculo;
import br.com.estacionamento.api.repository.EstadiaRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import br.com.estacionamento.api.repository.VagaRepository;
import br.com.estacionamento.api.repository.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EstadiaService {

    private static final String[] MESES_PT = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };
    private static final long RELATORIO_CACHE_TTL_MS = 2 * 60 * 1000;

    private final EstadiaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final VeiculoRepository veiculoRepository;
    private final VagaRepository vagaRepository;
    private final EstacionamentoService estacionamentoService;

    private final Map<Long, RelatorioCacheEntry> relatorioCache = new ConcurrentHashMap<>();

    public EstadiaService(EstadiaRepository repository, UsuarioRepository usuarioRepository,
                          VeiculoRepository veiculoRepository, VagaRepository vagaRepository,
                          EstacionamentoService estacionamentoService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.veiculoRepository = veiculoRepository;
        this.vagaRepository = vagaRepository;
        this.estacionamentoService = estacionamentoService;
    }

    // AGORA É ISOLADO
    public List<Estadia> listarAtivasPorOperador(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operador não encontrado"));
                
        if (usuario.getEstacionamento() == null) {
            throw new RuntimeException("Este operador não possui um estacionamento vinculado.");
        }
        
        return repository.findByAtivaTrueAndVagaEstacionamentoId(usuario.getEstacionamento().getId());
    }

    // Histórico do motorista: estadias já encerradas (finalizadas ou canceladas)
    public List<Estadia> buscarHistoricoDoMotorista(String email) {
        return repository.findByVeiculoUsuarioEmailAndAtivaFalseOrderByIdDesc(email);
    }

    // Histórico do operador: estadias encerradas (finalizadas ou canceladas) do seu estacionamento
    public List<Estadia> listarHistoricoPorOperador(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operador não encontrado"));

        if (usuario.getEstacionamento() == null) {
            throw new RuntimeException("Este operador não possui um estacionamento vinculado.");
        }

        return repository.findByVagaEstacionamentoIdAndAtivaFalseOrderByIdDesc(usuario.getEstacionamento().getId());
    }

    public Estadia buscarEstadiaAtivaDoMotorista(String email) {
        List<Estadia> resultados = repository.findAtivaOuCanceladaNaoLidaByVeiculoUsuarioEmail(email);

        if (resultados.isEmpty()) {
            throw new RecursoNaoEncontradoException("Nenhuma estadia ativa encontrada.");
        }

        Estadia estadia = resultados.get(0);

        // Garante que o cancelamento seja reportado ao motorista uma única vez
        if (estadia.isCancelada() && !estadia.isNotificacaoCancelamentoLida()) {
            estadia.setNotificacaoCancelamentoLida(true);
            repository.save(estadia);
        }

        return estadia;
    }

    // Registrar entrada manual (operador): o veículo não precisa estar previamente
    // cadastrado por um motorista — se a placa não existir, cria um veículo "avulso".
    @Transactional
    public Estadia registrarEntrada(String placa, Long vagaId) {
        if (placa == null || placa.isBlank()) {
            throw new RuntimeException("A placa é obrigatória");
        }

        String placaNormalizada = placa.trim().toUpperCase();
        if (placaNormalizada.length() > 10) {
            throw new RuntimeException("A placa deve ter no máximo 10 caracteres.");
        }

        Vaga vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada"));

        if (!vaga.isAtivo()) {
            throw new RuntimeException("Esta vaga não está disponível");
        }

        if (vaga.isOcupada()) {
            throw new RuntimeException("Esta vaga já está ocupada");
        }

        Veiculo veiculo = veiculoRepository.findByPlaca(placaNormalizada)
                .orElseGet(() -> {
                    Veiculo novo = new Veiculo();
                    novo.setPlaca(placaNormalizada);
                    novo.setTipo(vaga.getTipoVeiculo() != null ? vaga.getTipoVeiculo() : TipoVeiculo.CARRO);
                    novo.setAtivo(true);
                    return veiculoRepository.save(novo);
                });

        if (vaga.getTipoVeiculo() != null && vaga.getTipoVeiculo() != veiculo.getTipo()) {
            throw new RuntimeException("Esta vaga é exclusiva para veículos do tipo " + vaga.getTipoVeiculo());
        }

        vaga.setOcupada(true);
        vagaRepository.save(vaga);

        Estadia estadia = new Estadia();
        estadia.setVeiculo(veiculo);
        estadia.setVaga(vaga);
        estadia.setEntrada(LocalDateTime.now());
        estadia.setCriadoEm(LocalDateTime.now());
        estadia.setAtiva(true);

        return repository.save(estadia);
    }

    // Motorista reserva uma vaga específica para um dos seus veículos
    @Transactional
    public Estadia reservarVaga(String email, Long vagaId, Long veiculoId, String previsaoChegadaIso) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado"));

        if (!veiculo.getUsuario().getEmail().equals(email)) {
            throw new RuntimeException("Este veículo não pertence ao usuário logado");
        }

        if (repository.findByAtivaTrueAndVeiculoUsuarioEmail(email).isPresent()) {
            throw new RuntimeException("Você já possui uma estadia ativa ou uma reserva pendente");
        }

        Vaga vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada"));

        if (!vaga.isAtivo()) {
            throw new RuntimeException("Esta vaga não está disponível");
        }

        if (vaga.isOcupada()) {
            throw new RuntimeException("Esta vaga já está ocupada ou reservada");
        }

        if (vaga.getTipoVeiculo() != null && vaga.getTipoVeiculo() != veiculo.getTipo()) {
            throw new RuntimeException("Esta vaga é exclusiva para veículos do tipo " + vaga.getTipoVeiculo());
        }

        vaga.setOcupada(true);
        vagaRepository.save(vaga);

        Estadia estadia = new Estadia();
        estadia.setVeiculo(veiculo);
        estadia.setVaga(vaga);
        estadia.setAtiva(true);
        estadia.setPendente(true);
        estadia.setCodigo(gerarCodigo());
        estadia.setCriadoEm(LocalDateTime.now());
        estadia.setPrevisaoChegada(parseDataHoraIso(previsaoChegadaIso));

        return repository.save(estadia);
    }

    // Converte uma string ISO-8601 (com ou sem offset/"Z") para LocalDateTime no fuso do servidor
    private LocalDateTime parseDataHoraIso(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(valor).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(valor);
        }
    }

    // Operador confirma o check-in do motorista a partir do código da reserva
    public Estadia confirmarCheckin(String emailOperador, String codigo) {
        Usuario operador = usuarioRepository.findByEmail(emailOperador)
                .orElseThrow(() -> new RuntimeException("Operador não encontrado"));

        if (operador.getEstacionamento() == null) {
            throw new RuntimeException("Este operador não possui um estacionamento vinculado.");
        }

        Estadia estadia = repository.findByCodigoAndPendenteTrue(codigo)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada ou já utilizada"));

        if (!estadia.getVaga().getEstacionamento().getId().equals(operador.getEstacionamento().getId())) {
            throw new RuntimeException("Esta reserva não pertence ao seu estacionamento");
        }

        estadia.setEntrada(LocalDateTime.now());
        estadia.setPendente(false);

        return repository.save(estadia);
    }

    // Operador cancela uma reserva pendente (ainda sem check-in), liberando a vaga
    @Transactional
    public Estadia cancelarReserva(String emailOperador, Long estadiaId) {
        Usuario operador = usuarioRepository.findByEmail(emailOperador)
                .orElseThrow(() -> new RuntimeException("Operador não encontrado"));

        if (operador.getEstacionamento() == null) {
            throw new RuntimeException("Este operador não possui um estacionamento vinculado.");
        }

        Estadia estadia = repository.findById(estadiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        if (!estadia.getVaga().getEstacionamento().getId().equals(operador.getEstacionamento().getId())) {
            throw new RecursoNaoEncontradoException("Reserva não encontrada");
        }

        if (!estadia.isPendente()) {
            throw new RuntimeException("Esta reserva não pode ser cancelada pois já está em uso ou finalizada.");
        }

        estadia.setPendente(false);
        estadia.setAtiva(false);
        estadia.setCancelada(true);
        estadia.setNotificacaoCancelamentoLida(false);

        Vaga vaga = estadia.getVaga();
        vaga.setOcupada(false);
        vagaRepository.save(vaga);

        return repository.save(estadia);
    }

    private String gerarCodigo() {
        String codigo;
        do {
            codigo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (repository.existsByCodigoAndPendenteTrue(codigo));
        return codigo;
    }

    @Transactional
    public Estadia finalizarEstadia(Long id) {
        Estadia estadia = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estadia não encontrada"));

        if (!estadia.isAtiva()) {
            throw new RuntimeException("Esta estadia já está encerrada");
        }

        if (estadia.isPendente()) {
            throw new RuntimeException("Esta estadia ainda não teve o check-in confirmado");
        }

        estadia.setSaida(LocalDateTime.now());
        estadia.setAtiva(false);

        Vaga vaga = estadia.getVaga();
        vaga.setOcupada(false);
        vagaRepository.save(vaga);

        long horas = ChronoUnit.HOURS.between(estadia.getEntrada(), estadia.getSaida());
        if (horas == 0) horas = 1;

        Double valorHora = estacionamentoService.obterValorHora(vaga.getEstacionamento(), estadia.getVeiculo().getTipo());
        estadia.setValor(horas * valorHora);

        return repository.save(estadia);
    }

    // Relatório/dashboard do operador, com cache curto para evitar recálculo a cada acesso
    public RelatorioOperadorDTO gerarRelatorio(String email) {
        Estacionamento estacionamento = estacionamentoService.buscarMeuEstacionamento(email);
        Long estacionamentoId = estacionamento.getId();

        RelatorioCacheEntry cacheEntry = relatorioCache.get(estacionamentoId);
        long agora = System.currentTimeMillis();
        if (cacheEntry != null && (agora - cacheEntry.timestamp) < RELATORIO_CACHE_TTL_MS) {
            return cacheEntry.dto;
        }

        RelatorioOperadorDTO relatorio = montarRelatorio(estacionamento);
        relatorioCache.put(estacionamentoId, new RelatorioCacheEntry(relatorio, agora));
        return relatorio;
    }

    private RelatorioOperadorDTO montarRelatorio(Estacionamento estacionamento) {
        Long estacionamentoId = estacionamento.getId();
        LocalDateTime agora = LocalDateTime.now();

        // Faturamento em aberto: estadias em uso, calculado pelo tempo decorrido até agora
        List<Estadia> emAndamento = repository.findByVagaEstacionamentoIdAndAtivaTrueAndPendenteFalse(estacionamentoId);
        double aberto = 0.0;
        for (Estadia e : emAndamento) {
            if (e.getEntrada() == null) continue;
            long horas = ChronoUnit.HOURS.between(e.getEntrada(), agora);
            if (horas == 0) horas = 1;
            Double valorHora = estacionamentoService.obterValorHora(estacionamento, e.getVeiculo().getTipo());
            aberto += horas * valorHora;
        }

        // Janela de 12 meses cobre semana/mês/ano e os gráficos
        LocalDateTime inicio12Meses = YearMonth.from(agora.toLocalDate()).minusMonths(11).atDay(1).atStartOfDay();
        List<Estadia> finalizadas = repository.findByVagaEstacionamentoIdAndSaidaIsNotNullAndSaidaGreaterThanEqual(estacionamentoId, inicio12Meses);

        LocalDateTime inicioSemana = agora.toLocalDate().minusDays(6).atStartOfDay();
        LocalDateTime inicioMes = agora.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioAno = agora.toLocalDate().withDayOfYear(1).atStartOfDay();

        double semana = 0;
        double mes = 0;
        double ano = 0;
        for (Estadia e : finalizadas) {
            double valor = e.getValor() != null ? e.getValor() : 0.0;
            if (!e.getSaida().isBefore(inicioSemana)) semana += valor;
            if (!e.getSaida().isBefore(inicioMes)) mes += valor;
            if (!e.getSaida().isBefore(inicioAno)) ano += valor;
        }

        // Fluxo dos últimos 7 dias (entradas x saídas)
        List<Estadia> entradasRecentes = repository.findByVagaEstacionamentoIdAndEntradaIsNotNullAndEntradaGreaterThanEqual(estacionamentoId, inicioSemana);
        DateTimeFormatter fmtDia = DateTimeFormatter.ofPattern("dd/MM");
        List<FluxoDiaDTO> fluxoSemanal = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = agora.toLocalDate().minusDays(i);
            long entradasCount = entradasRecentes.stream()
                    .filter(e -> e.getEntrada().toLocalDate().equals(dia))
                    .count();
            long saidasCount = finalizadas.stream()
                    .filter(e -> e.getSaida().toLocalDate().equals(dia))
                    .count();
            fluxoSemanal.add(new FluxoDiaDTO(dia.format(fmtDia), entradasCount, saidasCount));
        }

        // Faturamento dos últimos 12 meses
        List<FaturamentoMensalDTO> faturamentoMensal = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth mesAlvo = YearMonth.from(agora.toLocalDate()).minusMonths(i);
            double total = finalizadas.stream()
                    .filter(e -> YearMonth.from(e.getSaida().toLocalDate()).equals(mesAlvo))
                    .mapToDouble(e -> e.getValor() != null ? e.getValor() : 0.0)
                    .sum();
            String label = MESES_PT[mesAlvo.getMonthValue() - 1] + "/" + String.format("%02d", mesAlvo.getYear() % 100);
            faturamentoMensal.add(new FaturamentoMensalDTO(label, arredondar(total)));
        }

        FaturamentoDTO faturamento = new FaturamentoDTO(arredondar(aberto), arredondar(semana), arredondar(mes), arredondar(ano));
        return new RelatorioOperadorDTO(faturamento, fluxoSemanal, faturamentoMensal);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static class RelatorioCacheEntry {
        final RelatorioOperadorDTO dto;
        final long timestamp;

        RelatorioCacheEntry(RelatorioOperadorDTO dto, long timestamp) {
            this.dto = dto;
            this.timestamp = timestamp;
        }
    }
}