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

@Service
public class EstadiaService {

    private final EstadiaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final VeiculoRepository veiculoRepository;
    private final VagaRepository vagaRepository;

    public EstadiaService(EstadiaRepository repository, UsuarioRepository usuarioRepository, 
                          VeiculoRepository veiculoRepository, VagaRepository vagaRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.veiculoRepository = veiculoRepository;
        this.vagaRepository = vagaRepository;
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

        vaga.setOcupada(true);
        vagaRepository.save(vaga);

        Estadia estadia = new Estadia();
        estadia.setVeiculo(veiculo);
        estadia.setVaga(vaga);
        estadia.setEntrada(LocalDateTime.now());
        estadia.setAtiva(true);

        return repository.save(estadia);
    }

    public Estadia finalizarEstadia(Long id) {
        Estadia estadia = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estadia não encontrada"));

        if (!estadia.isAtiva()) {
            throw new RuntimeException("Esta estadia já está encerrada");
        }

        estadia.setSaida(LocalDateTime.now());
        estadia.setAtiva(false);

        Vaga vaga = estadia.getVaga();
        vaga.setOcupada(false);
        vagaRepository.save(vaga);

        long horas = ChronoUnit.HOURS.between(estadia.getEntrada(), estadia.getSaida());
        if (horas == 0) horas = 1; 
        
        Double valorHora = vaga.getEstacionamento().getValorHora();
        estadia.setValor(horas * valorHora);

        return repository.save(estadia);
    }
}