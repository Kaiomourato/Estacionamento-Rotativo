package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.service.VagaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vagas")
public class VagaController {

    private final VagaService service;

    public VagaController(VagaService service) {
        this.service = service;
    }

    // Cadastrar vaga
    @PostMapping
    public ResponseEntity<Vaga> cadastrar(@RequestBody Vaga vaga) {
        return ResponseEntity.ok(service.cadastrar(vaga));
    }

    // Listar todas as vagas
    @GetMapping
    public ResponseEntity<List<Vaga>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    // Buscar vaga por ID
    @GetMapping("/{id}")
    public ResponseEntity<Vaga> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // Ocupar vaga
    @PutMapping("/{id}/ocupar")
    public ResponseEntity<Vaga> ocupar(@PathVariable Long id) {
        return ResponseEntity.ok(service.ocuparVaga(id));
    }

    // Liberar vaga
    @PutMapping("/{id}/liberar")
    public ResponseEntity<Vaga> liberar(@PathVariable Long id) {
        return ResponseEntity.ok(service.liberarVaga(id));
    }
}
