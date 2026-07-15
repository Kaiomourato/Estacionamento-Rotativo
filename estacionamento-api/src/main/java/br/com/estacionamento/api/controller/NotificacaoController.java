package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.dto.NotificacaoDTO;
import br.com.estacionamento.api.service.NotificacaoService;
import br.com.estacionamento.api.service.PushNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService service;
    private final PushNotificationService pushNotificationService;

    public NotificacaoController(NotificacaoService service, PushNotificationService pushNotificationService) {
        this.service = service;
        this.pushNotificationService = pushNotificationService;
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

    // Chamado pelo frontend após o usuário aceitar a permissão de notificações e o
    // Firebase gerar o token de push deste navegador/dispositivo.
    @PostMapping("/dispositivo")
    public ResponseEntity<Void> registrarDispositivo(@RequestBody Map<String, String> body, Principal principal) {
        pushNotificationService.registrarDispositivo(principal.getName(), body.get("token"));
        return ResponseEntity.ok().build();
    }

    // Chamado ao revogar a permissão/desativar notificações no app, para não enviar push
    // para um token que o usuário não quer mais receber.
    @DeleteMapping("/dispositivo")
    public ResponseEntity<Void> removerDispositivo(@RequestBody Map<String, String> body) {
        pushNotificationService.removerDispositivo(body.get("token"));
        return ResponseEntity.ok().build();
    }
}
