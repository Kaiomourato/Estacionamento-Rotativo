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

    @PostMapping
    public ResponseEntity<Estadia> iniciar(
            @RequestParam String placa,
            @RequestParam Long vagaId
    ) {
        return ResponseEntity.ok(service.iniciar(placa, vagaId));
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Estadia> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(service.finalizar(id));
    }

    @GetMapping("/ativas")
    public ResponseEntity<List<Estadia>> listarAtivas() {
        return ResponseEntity.ok(service.listarAtivas());
    }

    @GetMapping("/{id}/valor")
    public ResponseEntity<Double> consultarValor(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultarValor(id));
    }

    // NOVO: Rota para o motorista acompanhar sua estadia em tempo real
    @GetMapping("/minha-ativa")
    public ResponseEntity<Estadia> buscarMinhaEstadiaAtiva(Principal principal) {
        String emailLogado = principal.getName();
        Estadia estadia = service.buscarAtivaPorUsuario(emailLogado);
        return ResponseEntity.ok(estadia);
    }
}