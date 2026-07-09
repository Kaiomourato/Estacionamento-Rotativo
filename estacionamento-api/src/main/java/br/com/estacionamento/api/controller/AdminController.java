package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.dto.DashboardAdminDTO;
import br.com.estacionamento.api.dto.UsuarioResumoDTO;
import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.service.AdminDashboardService;
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

    private final AdminDashboardService service;

    public AdminController(AdminDashboardService service) {
        this.service = service;
    }

    // Cards + gráficos do dashboard. "inicio"/"fim" filtram os indicadores derivados de
    // período (check-ins, check-outs, faturamento do período, acessos); sem eles, usa os
    // últimos 30 dias. "estacionamentoId" recorta os mesmos indicadores para um único
    // estacionamento; omitido, considera todos. O bloco financeiro (aberto/semana/mês/ano
    // e os gráficos de 7 dias / 12 meses) segue sempre a mesma janela fixa do painel do operador.
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardAdminDTO> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long estacionamentoId) {
        return ResponseEntity.ok(service.montarDashboard(inicio, fim, estacionamentoId));
    }

    // Últimos usuários cadastrados (tabela)
    @GetMapping("/usuarios/recentes")
    public ResponseEntity<List<UsuarioResumoDTO>> usuariosRecentes() {
        return ResponseEntity.ok(service.listarUsuariosRecentes());
    }

    // Últimos check-ins (tabela), opcionalmente filtrado por estacionamento
    @GetMapping("/estadias/ultimos-checkins")
    public ResponseEntity<List<Estadia>> ultimosCheckins(
            @RequestParam(required = false) Long estacionamentoId,
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(service.listarUltimosCheckins(estacionamentoId, limite));
    }

    // Últimos check-outs (tabela), opcionalmente filtrado por estacionamento
    @GetMapping("/estadias/ultimos-checkouts")
    public ResponseEntity<List<Estadia>> ultimosCheckouts(
            @RequestParam(required = false) Long estacionamentoId,
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(service.listarUltimosCheckouts(estacionamentoId, limite));
    }
}
