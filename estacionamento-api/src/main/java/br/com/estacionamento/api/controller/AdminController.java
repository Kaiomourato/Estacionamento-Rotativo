package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.dto.DashboardAdminDTO;
import br.com.estacionamento.api.dto.UsuarioResumoDTO;
import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.service.AdminDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// Painel administrativo: visão consolidada de todos os estacionamentos, restrita a ADMIN.
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminDashboardService service;

    public AdminController(AdminDashboardService service) {
        this.service = service;
    }

    // Cards, indicadores e gráficos do dashboard. "estacionamentoId" recorta tudo para um
    // único estacionamento; omitido, considera a rede inteira. As janelas de tempo (hoje,
    // semana, mês, ano, últimos 12 meses) são sempre fixas — sem filtro de período arbitrário.
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardAdminDTO> dashboard(@RequestParam(required = false) Long estacionamentoId) {
        // DIAGNÓSTICO TEMPORÁRIO — investigação do 500 intermitente em produção. Remover
        // depois de identificada a causa raiz (ver AdminDashboardService/AuditoriaService).
        long inicio = System.currentTimeMillis();
        log.info("[DIAG] GET /admin/dashboard iniciado (estacionamentoId={})", estacionamentoId);
        try {
            DashboardAdminDTO resultado = service.montarDashboard(estacionamentoId);
            log.info("[DIAG] GET /admin/dashboard concluído em {}ms", System.currentTimeMillis() - inicio);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("[DIAG] GET /admin/dashboard FALHOU após {}ms — {}: {}",
                    System.currentTimeMillis() - inicio, e.getClass().getName(), e.getMessage(), e);
            throw e;
        }
    }

    // Últimos usuários cadastrados (widget do dashboard)
    @GetMapping("/usuarios/recentes")
    public ResponseEntity<List<UsuarioResumoDTO>> usuariosRecentes() {
        return ResponseEntity.ok(service.listarUsuariosRecentes());
    }

    // Página Usuários (e Operadores, filtrando role=OPERADOR): listagem paginada e pesquisável
    @GetMapping("/usuarios")
    public ResponseEntity<Page<UsuarioResumoDTO>> usuarios(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String busca,
            Pageable pageable) {
        // DIAGNÓSTICO TEMPORÁRIO — ver comentário acima
        long inicio = System.currentTimeMillis();
        log.info("[DIAG] GET /admin/usuarios iniciado (role={}, busca={}, page={}, size={})",
                role, busca, pageable.getPageNumber(), pageable.getPageSize());
        try {
            Page<UsuarioResumoDTO> resultado = service.listarUsuariosPaginado(role, busca, pageable);
            log.info("[DIAG] GET /admin/usuarios concluído em {}ms (totalElements={})",
                    System.currentTimeMillis() - inicio, resultado.getTotalElements());
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("[DIAG] GET /admin/usuarios FALHOU após {}ms — {}: {}",
                    System.currentTimeMillis() - inicio, e.getClass().getName(), e.getMessage(), e);
            throw e;
        }
    }

    // Página Vagas: todas as vagas de todos os estacionamentos, paginada e pesquisável
    @GetMapping("/vagas")
    public ResponseEntity<Page<Vaga>> vagas(
            @RequestParam(required = false) Long estacionamentoId,
            @RequestParam(required = false) String busca,
            Pageable pageable) {
        // DIAGNÓSTICO TEMPORÁRIO — ver comentário acima
        long inicio = System.currentTimeMillis();
        log.info("[DIAG] GET /admin/vagas iniciado (estacionamentoId={}, busca={}, page={}, size={})",
                estacionamentoId, busca, pageable.getPageNumber(), pageable.getPageSize());
        try {
            Page<Vaga> resultado = service.listarVagasPaginado(estacionamentoId, busca, pageable);
            log.info("[DIAG] GET /admin/vagas concluído em {}ms (totalElements={})",
                    System.currentTimeMillis() - inicio, resultado.getTotalElements());
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("[DIAG] GET /admin/vagas FALHOU após {}ms — {}: {}",
                    System.currentTimeMillis() - inicio, e.getClass().getName(), e.getMessage(), e);
            throw e;
        }
    }

    // Página Pagamentos: estadias finalizadas, com filtro opcional de estacionamento e período
    @GetMapping("/pagamentos")
    public ResponseEntity<Page<Estadia>> pagamentos(
            @RequestParam(required = false) Long estacionamentoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Pageable pageable) {
        // DIAGNÓSTICO TEMPORÁRIO — ver comentário acima
        long inicioMs = System.currentTimeMillis();
        log.info("[DIAG] GET /admin/pagamentos iniciado (estacionamentoId={}, inicio={}, fim={}, page={}, size={})",
                estacionamentoId, inicio, fim, pageable.getPageNumber(), pageable.getPageSize());
        try {
            Page<Estadia> resultado = service.listarPagamentosPaginado(estacionamentoId, inicio, fim, pageable);
            log.info("[DIAG] GET /admin/pagamentos concluído em {}ms (totalElements={})",
                    System.currentTimeMillis() - inicioMs, resultado.getTotalElements());
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("[DIAG] GET /admin/pagamentos FALHOU após {}ms — {}: {}",
                    System.currentTimeMillis() - inicioMs, e.getClass().getName(), e.getMessage(), e);
            throw e;
        }
    }

    // Últimos check-ins (tabela do dashboard), opcionalmente filtrado por estacionamento
    @GetMapping("/estadias/ultimos-checkins")
    public ResponseEntity<List<Estadia>> ultimosCheckins(
            @RequestParam(required = false) Long estacionamentoId,
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(service.listarUltimosCheckins(estacionamentoId, limite));
    }

    // Últimos check-outs (tabela do dashboard), opcionalmente filtrado por estacionamento
    @GetMapping("/estadias/ultimos-checkouts")
    public ResponseEntity<List<Estadia>> ultimosCheckouts(
            @RequestParam(required = false) Long estacionamentoId,
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(service.listarUltimosCheckouts(estacionamentoId, limite));
    }
}
