package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.model.Veiculo;
import br.com.estacionamento.api.service.VeiculoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/veiculos")
public class VeiculoController {

    private final VeiculoService service;

    public VeiculoController(VeiculoService service) {
        this.service = service;
    }

    // Criar veículo atrelado ao usuário logado
    @PostMapping
    public ResponseEntity<Veiculo> cadastrar(@RequestBody @Valid Veiculo veiculo, Principal principal) {
        String emailLogado = principal.getName();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrarComUsuario(veiculo, emailLogado));
    }

    // Listar APENAS os veículos do usuário logado
    @GetMapping("/meus")
    public ResponseEntity<List<Veiculo>> listarMeusVeiculos(Principal principal) {
        String emailLogado = principal.getName();
        return ResponseEntity.ok(service.listarPorUsuario(emailLogado));
    }

    // Listagem completa: restrita a administradores (expõe veículos de todos os usuários)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Veiculo>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }
}