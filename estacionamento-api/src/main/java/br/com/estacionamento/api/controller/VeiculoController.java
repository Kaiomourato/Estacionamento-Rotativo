package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.model.Veiculo;
import br.com.estacionamento.api.service.VeiculoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/veiculos")
@CrossOrigin(origins = "http://localhost:5173") 
public class VeiculoController {

    private final VeiculoService service;

    public VeiculoController(VeiculoService service) {
        this.service = service;
    }

    // Criar veículo atrelado ao usuário logado
    @PostMapping
    public ResponseEntity<Veiculo> cadastrar(@RequestBody Veiculo veiculo, Principal principal) {
        String emailLogado = principal.getName();
        return ResponseEntity.ok(service.cadastrarComUsuario(veiculo, emailLogado));
    }

    // Listar APENAS os veículos do usuário logado
    @GetMapping("/meus")
    public ResponseEntity<List<Veiculo>> listarMeusVeiculos(Principal principal) {
        String emailLogado = principal.getName();
        return ResponseEntity.ok(service.listarPorUsuario(emailLogado));
    }

    @GetMapping
    public ResponseEntity<List<Veiculo>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }
}