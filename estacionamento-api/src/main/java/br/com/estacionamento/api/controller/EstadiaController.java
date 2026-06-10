package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.service.EstadiaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/estadias")
public class EstadiaController {
    
    private final EstadiaService service;

    public EstadiaController(EstadiaService service) {
        this.service = service;
    }

    // Rota do Motorista
    @GetMapping("/minha-ativa")
    public ResponseEntity<Estadia> buscarMinhaAtiva(Principal principal) {
        return ResponseEntity.ok(service.buscarEstadiaAtivaDoMotorista(principal.getName()));
    }

    // AGORA É ISOLADO: Rota do Operador lista só os carros do pátio dele
    @GetMapping("/ativas")
    public ResponseEntity<List<Estadia>> listarAtivas(Principal principal) {
        return ResponseEntity.ok(service.listarAtivasPorOperador(principal.getName()));
    }

    @PostMapping
    public ResponseEntity<Estadia> registrarEntrada(@RequestParam String placa, @RequestParam Long vagaId) {
        return ResponseEntity.ok(service.registrarEntrada(placa, vagaId));
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Estadia> finalizarEstadia(@PathVariable Long id) {
        return ResponseEntity.ok(service.finalizarEstadia(id));
    }

    // Rota do Motorista: agenda uma vaga específica para um dos seus veículos
    @PostMapping("/reservar")
    public ResponseEntity<Estadia> reservar(@RequestParam Long vagaId, @RequestParam Long veiculoId,
            @RequestParam(required = false) String previsaoChegada, Principal principal) {
        return ResponseEntity.ok(service.reservarVaga(principal.getName(), vagaId, veiculoId, previsaoChegada));
    }

    // Rota do Operador: confirma o check-in do motorista a partir do código da reserva
    @PutMapping("/checkin")
    public ResponseEntity<Estadia> confirmarCheckin(@RequestParam String codigo, Principal principal) {
        return ResponseEntity.ok(service.confirmarCheckin(principal.getName(), codigo));
    }

    // Rota do Operador: cancela uma reserva pendente (sem check-in), liberando a vaga
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Estadia> cancelar(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(service.cancelarReserva(principal.getName(), id));
    }
}