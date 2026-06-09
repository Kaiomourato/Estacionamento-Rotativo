package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.service.EstadiaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/estadias")
public class EstadiaController {

    private final EstadiaService service;

    public EstadiaController(EstadiaService service) {
        this.service = service;
    }

    // ── Motorista ─────────────────────────────────────────────────────────────
    @GetMapping("/minha-ativa")
    public ResponseEntity<Estadia> buscarMinhaAtiva(Principal principal) {
        return ResponseEntity.ok(service.buscarEstadiaAtivaDoMotorista(principal.getName()));
    }

    @GetMapping("/meu-historico")
    public ResponseEntity<List<Estadia>> meuHistorico(Principal principal) {
        return ResponseEntity.ok(service.historicoDoMotorista(principal.getName()));
    }

    // ── Operador ──────────────────────────────────────────────────────────────
    @GetMapping("/ativas")
    public ResponseEntity<List<Estadia>> listarAtivas(Principal principal) {
        return ResponseEntity.ok(service.listarAtivasPorOperador(principal.getName()));
    }

    @GetMapping("/historico")
    public ResponseEntity<List<Estadia>> historico(Principal principal) {
        return ResponseEntity.ok(service.historicoDoOperador(principal.getName()));
    }

    // ── Entrada manual (operador informa placa + vaga) ────────────────────────
    @PostMapping
    public ResponseEntity<Estadia> registrarEntrada(
            @RequestParam String placa,
            @RequestParam Long vagaId) {
        return ResponseEntity.ok(service.registrarEntrada(placa, vagaId));
    }

    // ── Reserva (motorista reserva via app) ───────────────────────────────────
    @PostMapping("/reservar")
    public ResponseEntity<Estadia> reservar(
            @RequestBody Map<String, Object> body,
            Principal principal) {
        Long vagaId = Long.valueOf(body.get("vagaId").toString());
        return ResponseEntity.ok(service.reservar(principal.getName(), vagaId));
    }

    // ── Check-in (operador valida código do motorista) ────────────────────────
    @PostMapping("/checkin")
    public ResponseEntity<Estadia> checkin(@RequestParam String codigo) {
        return ResponseEntity.ok(service.confirmarCheckin(codigo));
    }

    // ── Encerrar estadia ──────────────────────────────────────────────────────
    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Estadia> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(service.finalizarEstadia(id));
    }
}
