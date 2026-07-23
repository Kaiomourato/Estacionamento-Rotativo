package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.Favorito;
import br.com.estacionamento.api.service.FavoritoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/favoritos")
public class FavoritoController {

    private final FavoritoService service;

    public FavoritoController(FavoritoService service) {
        this.service = service;
    }

    // Lista APENAS os estacionamentos favoritados pelo usuário logado
    @GetMapping("/meus")
    public ResponseEntity<List<Estacionamento>> listarMeus(Principal principal) {
        return ResponseEntity.ok(service.listarPorUsuario(principal.getName()));
    }

    // Idempotente: favoritar de novo o mesmo estacionamento não duplica
    @PostMapping("/{estacionamentoId}")
    public ResponseEntity<Favorito> adicionar(@PathVariable Long estacionamentoId, Principal principal) {
        return ResponseEntity.ok(service.adicionar(principal.getName(), estacionamentoId));
    }

    // Idempotente: remover um favorito que já não existe não é erro
    @DeleteMapping("/{estacionamentoId}")
    public ResponseEntity<Void> remover(@PathVariable Long estacionamentoId, Principal principal) {
        service.remover(principal.getName(), estacionamentoId);
        return ResponseEntity.noContent().build();
    }
}
