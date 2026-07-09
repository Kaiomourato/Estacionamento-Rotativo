package br.com.estacionamento.api.service;

import br.com.estacionamento.api.model.LogAcesso;
import br.com.estacionamento.api.model.TipoEventoLog;
import br.com.estacionamento.api.repository.LogAcessoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AuditoriaService {

    private static final DateTimeFormatter FMT_CSV = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final LogAcessoRepository repository;

    public AuditoriaService(LogAcessoRepository repository) {
        this.repository = repository;
    }

    public Page<LogAcesso> buscar(String usuarioEmail, LocalDate dataInicial, LocalDate dataFinal,
                                   String tipoEvento, String rota, String role, Integer status, Pageable pageable) {
        LocalDateTime desde = dataInicial != null ? dataInicial.atStartOfDay() : null;
        LocalDateTime ate = dataFinal != null ? dataFinal.plusDays(1).atStartOfDay() : null;
        TipoEventoLog tipo = parseTipoEvento(tipoEvento);
        return repository.buscarComFiltros(vazioParaNulo(usuarioEmail), desde, ate, tipo, vazioParaNulo(rota),
                vazioParaNulo(role), status, pageable);
    }

    // Mesma busca, sem paginação — usada pela exportação em CSV
    public List<LogAcesso> buscarParaExportar(String usuarioEmail, LocalDate dataInicial, LocalDate dataFinal,
                                               String tipoEvento, String rota, String role, Integer status) {
        return buscar(usuarioEmail, dataInicial, dataFinal, tipoEvento, rota, role, status, Pageable.unpaged()).getContent();
    }

    public String gerarCsv(List<LogAcesso> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Data/Hora,Usuário,Role,Tipo de Evento,Descrição,Método,Endpoint,Status,IP,Navegador,Sistema Operacional\n");
        for (LogAcesso log : logs) {
            sb.append(csv(log.getDataHora() != null ? log.getDataHora().format(FMT_CSV) : "")).append(',')
              .append(csv(log.getUsuarioEmail())).append(',')
              .append(csv(log.getRole())).append(',')
              .append(csv(log.getTipoEvento() != null ? log.getTipoEvento().name() : "")).append(',')
              .append(csv(log.getDescricao())).append(',')
              .append(csv(log.getMetodo())).append(',')
              .append(csv(log.getRota())).append(',')
              .append(csv(log.getStatus() != null ? log.getStatus().toString() : "")).append(',')
              .append(csv(log.getIp())).append(',')
              .append(csv(log.getNavegador())).append(',')
              .append(csv(log.getSistemaOperacional())).append('\n');
        }
        return sb.toString();
    }

    private String csv(String valor) {
        if (valor == null) return "";
        String escapado = valor.replace("\"", "\"\"");
        return "\"" + escapado + "\"";
    }

    private TipoEventoLog parseTipoEvento(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return TipoEventoLog.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String vazioParaNulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor;
    }
}
