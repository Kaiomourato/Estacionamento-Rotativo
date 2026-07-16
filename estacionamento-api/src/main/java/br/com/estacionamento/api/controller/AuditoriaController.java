package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.model.LogAcesso;
import br.com.estacionamento.api.model.TipoEventoLog;
import br.com.estacionamento.api.service.AuditoriaService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

// Tela de Auditoria: histórico pesquisável de todas as requisições registradas
// pelo LogAcessoFilter, restrito a ADMIN.
@RestController
@RequestMapping("/admin/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaController.class);

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<LogAcesso>> buscar(
            @RequestParam(required = false) String usuarioEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) String tipoEvento,
            @RequestParam(required = false) String rota,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            Pageable pageable) {
        // DIAGNÓSTICO TEMPORÁRIO — investigação do 500 intermitente em produção. Remover
        // depois de identificada a causa raiz.
        long inicio = System.currentTimeMillis();
        log.info("[DIAG] GET /admin/auditoria iniciado (usuarioEmail={}, tipoEvento={}, role={}, status={}, page={}, size={})",
                usuarioEmail, tipoEvento, role, status, pageable.getPageNumber(), pageable.getPageSize());
        try {
            Page<LogAcesso> resultado = service.buscar(usuarioEmail, dataInicial, dataFinal, tipoEvento, rota, role, status, pageable);
            log.info("[DIAG] GET /admin/auditoria concluído em {}ms (totalElements={})",
                    System.currentTimeMillis() - inicio, resultado.getTotalElements());
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("[DIAG] GET /admin/auditoria FALHOU após {}ms — {}: {}",
                    System.currentTimeMillis() - inicio, e.getClass().getName(), e.getMessage(), e);
            throw e;
        }
    }

    // Valores fixos do enum, para popular o filtro "Tipo de evento" no frontend
    @GetMapping("/tipos-evento")
    public ResponseEntity<TipoEventoLog[]> tiposEvento() {
        return ResponseEntity.ok(TipoEventoLog.values());
    }

    @GetMapping("/exportar.csv")
    public void exportarCsv(
            @RequestParam(required = false) String usuarioEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) String tipoEvento,
            @RequestParam(required = false) String rota,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {
        List<LogAcesso> logs = service.buscarParaExportar(usuarioEmail, dataInicial, dataFinal, tipoEvento, rota, role, status);
        String csv = service.gerarCsv(logs);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"auditoria.csv\"");
        // BOM UTF-8: garante acentuação correta ao abrir o CSV direto no Excel
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);
        response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }
}
