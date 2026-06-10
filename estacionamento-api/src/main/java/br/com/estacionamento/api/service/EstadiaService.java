package br.com.estacionamento.api.service;

import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.model.Veiculo;
import br.com.estacionamento.api.repository.EstadiaRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import br.com.estacionamento.api.repository.VagaRepository;
import br.com.estacionamento.api.repository.VeiculoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class EstadiaService {

    private final EstadiaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final VeiculoRepository veiculoRepository;
    private final VagaRepository vagaRepository;
    private final EstacionamentoService estacionamentoService;

    public EstadiaService(EstadiaRepository repository, UsuarioRepository usuarioRepository,
                          VeiculoRepository veiculoRepository, VagaRepository vagaRepository,
                          EstacionamentoService estacionamentoService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.veiculoRepository = veiculoRepository;
        this.vagaRepository = vagaRepository;
        this.estacionamentoService = estacionamentoService;
    }

    // AGORA É ISOLADO
    public List<Estadia> listarAtivasPorOperador(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operador não encontrado"));
                
        if (usuario.getEstacionamento() == null) {
            throw new RuntimeException("Este operador não possui um estacionamento vinculado.");
        }
        
        return repository.findByAtivaTrueAndVagaEstacionamentoId(usuario.getEstacionamento().getId());
    }

    public Estadia buscarEstadiaAtivaDoMotorista(String email) {
        return repository.findByAtivaTrueAndVeiculoUsuarioEmail(email)
                .orElseThrow(() -> new RuntimeException("Nenhuma estadia ativa encontrada."));
    }

    public Estadia registrarEntrada(String placa, Long vagaId) {
        Veiculo veiculo = veiculoRepository.findByPlaca(placa)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));
                
        Vaga vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada"));

        if (vaga.isOcupada()) {
            throw new RuntimeException("Esta vaga já está ocupada");
        }

        if (vaga.getTipoVeiculo() != null && vaga.getTipoVeiculo() != veiculo.getTipo()) {
            throw new RuntimeException("Esta vaga é exclusiva para veículos do tipo " + vaga.getTipoVeiculo());
        }

        vaga.setOcupada(true);
        vagaRepository.save(vaga);

        Estadia estadia = new Estadia();
        estadia.setVeiculo(veiculo);
        estadia.setVaga(vaga);
        estadia.setEntrada(LocalDateTime.now());
        estadia.setAtiva(true);

        return repository.save(estadia);
    }

    // Motorista reserva uma vaga específica para um dos seus veículos
    public Estadia reservarVaga(String email, Long vagaId, Long veiculoId) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));

        if (!veiculo.getUsuario().getEmail().equals(email)) {
            throw new RuntimeException("Este veículo não pertence ao usuário logado");
        }

        if (repository.findByAtivaTrueAndVeiculoUsuarioEmail(email).isPresent()) {
            throw new RuntimeException("Você já possui uma estadia ativa ou uma reserva pendente");
        }

        Vaga vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada"));

        if (!vaga.isAtivo()) {
            throw new RuntimeException("Esta vaga não está disponível");
        }

        if (vaga.isOcupada()) {
            throw new RuntimeException("Esta vaga já está ocupada ou reservada");
        }

        if (vaga.getTipoVeiculo() != null && vaga.getTipoVeiculo() != veiculo.getTipo()) {
            throw new RuntimeException("Esta vaga é exclusiva para veículos do tipo " + vaga.getTipoVeiculo());
        }

        vaga.setOcupada(true);
        vagaRepository.save(vaga);

        Estadia estadia = new Estadia();
        estadia.setVeiculo(veiculo);
        estadia.setVaga(vaga);
        estadia.setAtiva(true);
        estadia.setPendente(true);
        estadia.setCodigo(gerarCodigo());

        return repository.save(estadia);
    }

    // Operador confirma o check-in do motorista a partir do código da reserva
    public Estadia confirmarCheckin(String emailOperador, String codigo) {
        Usuario operador = usuarioRepository.findByEmail(emailOperador)
                .orElseThrow(() -> new RuntimeException("Operador não encontrado"));

        if (operador.getEstacionamento() == null) {
            throw new RuntimeException("Este operador não possui um estacionamento vinculado.");
        }

        Estadia estadia = repository.findByCodigoAndPendenteTrue(codigo)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada ou já utilizada"));

        if (!estadia.getVaga().getEstacionamento().getId().equals(operador.getEstacionamento().getId())) {
            throw new RuntimeException("Esta reserva não pertence ao seu estacionamento");
        }

        estadia.setEntrada(LocalDateTime.now());
        estadia.setPendente(false);

        return repository.save(estadia);
    }

    private String gerarCodigo() {
        String codigo;
        do {
            codigo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (repository.existsByCodigoAndPendenteTrue(codigo));
        return codigo;
    }

    public Estadia finalizarEstadia(Long id) {
        Estadia estadia = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estadia não encontrada"));

        if (!estadia.isAtiva()) {
            throw new RuntimeException("Esta estadia já está encerrada");
        }

        if (estadia.isPendente()) {
            throw new RuntimeException("Esta estadia ainda não teve o check-in confirmado");
        }

        estadia.setSaida(LocalDateTime.now());
        estadia.setAtiva(false);

        Vaga vaga = estadia.getVaga();
        vaga.setOcupada(false);
        vagaRepository.save(vaga);

        long horas = ChronoUnit.HOURS.between(estadia.getEntrada(), estadia.getSaida());
        if (horas == 0) horas = 1;

        Double valorHora = estacionamentoService.obterValorHora(vaga.getEstacionamento(), estadia.getVeiculo().getTipo());
        estadia.setValor(horas * valorHora);

        return repository.save(estadia);
    }
}