package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.model.TipoVeiculo;
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

    // Lista vagas do operador logado (painel operador)
    @GetMapping
    public ResponseEntity<List<Vaga>> listar(Principal principal) {
        return ResponseEntity.ok(service.listarPorOperador(principal.getName()));
    }

    // Lista vagas por estacionamento — usado pelo motorista para reservar (rota pública)
    @GetMapping("/por-estacionamento/{estacionamentoId}")
    public ResponseEntity<List<Vaga>> listarPorEstacionamento(@PathVariable Long estacionamentoId) {
        return ResponseEntity.ok(service.listarPorEstacionamento(estacionamentoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vaga> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
