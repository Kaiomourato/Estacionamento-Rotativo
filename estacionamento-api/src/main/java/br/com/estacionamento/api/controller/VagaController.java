package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.dto.ResumoVagasDTO;
import br.com.estacionamento.api.dto.VagaRequestDTO;
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

    // A vaga é sempre criada no estacionamento do operador logado
    @PostMapping
    public ResponseEntity<Vaga> cadastrar(@RequestBody VagaRequestDTO dto, Principal principal) {
        return ResponseEntity.ok(service.cadastrar(dto, principal.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vaga> atualizar(@PathVariable Long id, @RequestBody VagaRequestDTO dto, Principal principal) {
        return ResponseEntity.ok(service.atualizar(id, dto, principal.getName()));
    }

    // AGORA É ISOLADO: Lista apenas as vagas do estacionamento do operador logado
    @GetMapping
    public ResponseEntity<List<Vaga>> listar(Principal principal) {
        return ResponseEntity.ok(service.listarPorOperador(principal.getName()));
    }

    // Contagem de vagas (total/livres/ocupadas) por tipo de veículo
    @GetMapping("/resumo")
    public ResponseEntity<List<ResumoVagasDTO>> resumo(Principal principal) {
        return ResponseEntity.ok(service.resumoPorOperador(principal.getName()));
    }

    // Lista vagas de um estacionamento específico — usado pelo motorista para reservar
    @GetMapping("/por-estacionamento/{estacionamentoId}")
    public ResponseEntity<List<Vaga>> listarPorEstacionamento(@PathVariable Long estacionamentoId) {
        return ResponseEntity.ok(service.listarPorEstacionamento(estacionamentoId));
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
