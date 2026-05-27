package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.service.EstacionamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

import java.util.List;

@RestController
@RequestMapping("/estacionamentos")
public class EstacionamentoController {

    private final EstacionamentoService service;

    public EstacionamentoController(EstacionamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Estacionamento> listar() {
        return service.listarTodos();
    }

    @GetMapping("/proximos")
    public List<Estacionamento> buscarProximos(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "10.0") Double raio) {
        return service.buscarProximos(lat, lng, raio);
    }

    @PostMapping
    public ResponseEntity<Estacionamento> cadastrar(@RequestBody Estacionamento estacionamento) {
        return ResponseEntity.status(201).body(service.salvar(estacionamento));
    }

    // NOVO: Retorna o estacionamento do operador logado
    @GetMapping("/meu")
    public ResponseEntity<Estacionamento> buscarMeuEstacionamento(Principal principal) {
        return ResponseEntity.ok(service.buscarMeuEstacionamento(principal.getName()));
    }

    // NOVO: Atualiza os dados (usado na aba configurações)
    @PutMapping("/{id}")
    public ResponseEntity<Estacionamento> atualizar(@PathVariable Long id, @RequestBody Estacionamento dados) {
        return ResponseEntity.ok(service.atualizar(id, dados));
    }
}