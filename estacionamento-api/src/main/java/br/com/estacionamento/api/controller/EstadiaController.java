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
}