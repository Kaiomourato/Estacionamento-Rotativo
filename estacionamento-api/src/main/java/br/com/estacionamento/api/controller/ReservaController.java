package br.com.estacionamento.api.controller;

import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.service.EstadiaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final EstadiaService estadiaService;

    public ReservaController(EstadiaService estadiaService) {
        this.estadiaService = estadiaService;
    }

    // POST /reservas  { "vagaId": 1, "veiculoId": 2 }
    // Retorna a estadia com o codigoCheckin para o motorista apresentar ao operador
    @PostMapping
    public ResponseEntity<Estadia> criarReserva(
            @RequestBody Map<String, Long> body,
            Principal principal) {
        Long vagaId    = body.get("vagaId");
        Long veiculoId = body.get("veiculoId");
        if (vagaId == null || veiculoId == null)
            return ResponseEntity.badRequest().build();
        Estadia estadia = estadiaService.criarReserva(vagaId, principal.getName(), veiculoId);
        return ResponseEntity.ok(estadia);
    }
}
