package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.service.EstadiaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estadias")
public class EstadiaController {

    private final EstadiaService service;

    public EstadiaController(EstadiaService service) {
        this.service = service;
    }

    // Iniciar estadia
    @PostMapping
    public ResponseEntity<Estadia> iniciar(
            @RequestParam Long veiculoId,
            @RequestParam Long vagaId
    ) {
        return ResponseEntity.ok(service.iniciar(veiculoId, vagaId));
    }

    // Finalizar estadia
    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Estadia> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(service.finalizar(id));
    }

    // Listar estadias ativas
    @GetMapping("/ativas")
    public ResponseEntity<List<Estadia>> listarAtivas() {
        return ResponseEntity.ok(service.listarAtivas());
    }

    // Consultar valor atual
    @GetMapping("/{id}/valor")
    public ResponseEntity<Double> consultarValor(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultarValor(id));
    }
}
