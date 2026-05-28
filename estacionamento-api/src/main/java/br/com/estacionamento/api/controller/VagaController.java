package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.service.VagaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/vagas")
public class VagaController {

    private final VagaService service;

    public VagaController(VagaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Vaga> cadastrar(@RequestBody Vaga vaga) {
        return ResponseEntity.ok(service.cadastrar(vaga));
    }

    // AGORA É ISOLADO: Lista apenas as vagas do estacionamento do operador logado
    @GetMapping
    public ResponseEntity<List<Vaga>> listar(Principal principal) {
        return ResponseEntity.ok(service.listarPorOperador(principal.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vaga> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}/ocupar")
    public ResponseEntity<Vaga> ocupar(@PathVariable Long id) {
        return ResponseEntity.ok(service.ocuparVaga(id));
    }

    @PutMapping("/{id}/liberar")
    public ResponseEntity<Vaga> liberar(@PathVariable Long id) {
        return ResponseEntity.ok(service.liberarVaga(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}