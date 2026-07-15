package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.dto.NotificacaoDTO;
import br.com.estacionamento.api.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<NotificacaoDTO>> listar(Principal principal) {
        return ResponseEntity.ok(service.listar(principal.getName()));
    }

    @GetMapping("/nao-lidas/count")
    public ResponseEntity<Map<String, Long>> contarNaoLidas(Principal principal) {
        return ResponseEntity.ok(Map.of("total", service.contarNaoLidas(principal.getName())));
    }

    @PutMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long id, Principal principal) {
        service.marcarComoLida(principal.getName(), id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/lidas")
    public ResponseEntity<Void> marcarTodasComoLidas(Principal principal) {
        service.marcarTodasComoLidas(principal.getName());
        return ResponseEntity.ok().build();
    }
}
