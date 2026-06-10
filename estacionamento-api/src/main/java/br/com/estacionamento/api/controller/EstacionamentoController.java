package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.dto.PrecoVeiculoDTO;
import br.com.estacionamento.api.dto.RelatorioOperadorDTO;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.PrecoVeiculo;
import br.com.estacionamento.api.service.EstacionamentoService;
import br.com.estacionamento.api.service.EstadiaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

import java.util.List;

@RestController
@RequestMapping("/estacionamentos")
public class EstacionamentoController {

    private final EstacionamentoService service;
    private final EstadiaService estadiaService;

    public EstacionamentoController(EstacionamentoService service, EstadiaService estadiaService) {
        this.service = service;
        this.estadiaService = estadiaService;
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

    // Lista os valores de hora configurados por tipo de veículo
    @GetMapping("/meu/precos")
    public ResponseEntity<List<PrecoVeiculo>> listarPrecos(Principal principal) {
        return ResponseEntity.ok(service.listarPrecos(principal.getName()));
    }

    // Cria/atualiza os valores de hora por tipo de veículo. O operador pode
    // enviar o mesmo valor para todos os tipos ou personalizar cada um.
    @PutMapping("/meu/precos")
    public ResponseEntity<List<PrecoVeiculo>> atualizarPrecos(@RequestBody List<PrecoVeiculoDTO> precos, Principal principal) {
        return ResponseEntity.ok(service.atualizarPrecos(principal.getName(), precos));
    }

    // Dashboard do operador: faturamento (aberto/semana/mês/ano), fluxo semanal e faturamento mensal
    @GetMapping("/meu/relatorio")
    public ResponseEntity<RelatorioOperadorDTO> relatorio(Principal principal) {
        return ResponseEntity.ok(estadiaService.gerarRelatorio(principal.getName()));
    }
}
