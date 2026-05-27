package br.com.estacionamento.api.service;

import br.com.estacionamento.api.model.Estadia;
import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.model.Veiculo;
import br.com.estacionamento.api.repository.EstadiaRepository;
import br.com.estacionamento.api.repository.VagaRepository;
import br.com.estacionamento.api.repository.VeiculoRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EstadiaService {

    private final EstadiaRepository estadiaRepository;
    private final VeiculoRepository veiculoRepository;
    private final VagaRepository vagaRepository;

    // A regra de tempo por fração continua a mesma (exemplo: a cada 30 minutos)
    private static final int MINUTOS_POR_FRACAO = 30;

    public EstadiaService(
            EstadiaRepository estadiaRepository,
            VeiculoRepository veiculoRepository,
            VagaRepository vagaRepository
    ) {
        this.estadiaRepository = estadiaRepository;
        this.veiculoRepository = veiculoRepository;
        this.vagaRepository = vagaRepository;
    }

    public Estadia iniciar(String placa, Long vagaId) {

        Veiculo veiculo = veiculoRepository.findByPlaca(placa)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado com a placa: " + placa));

        Vaga vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada"));

        if (vaga.isOcupada()) {
            throw new RuntimeException("Vaga já está ocupada");
        }

        boolean veiculoJaEstacionado = estadiaRepository.findByAtivaTrue()
                .stream()
                .anyMatch(e -> e.getVeiculo().getId().equals(veiculo.getId()));

        if (veiculoJaEstacionado) {
            throw new RuntimeException("Este veículo já possui uma estadia ativa no pátio.");
        }

        Estadia estadia = new Estadia();
        estadia.setVeiculo(veiculo);
        estadia.setVaga(vaga);
        estadia.setEntrada(LocalDateTime.now());
        estadia.setAtiva(true);

        vaga.setOcupada(true);
        vagaRepository.save(vaga);

        return estadiaRepository.save(estadia);
    }

    public Estadia finalizar(Long estadiaId) {

        Estadia estadia = estadiaRepository.findById(estadiaId)
                .orElseThrow(() -> new RuntimeException("Estadia não encontrada"));

        if (!estadia.isAtiva()) {
            throw new RuntimeException("Estadia já finalizada");
        }

        estadia.setSaida(LocalDateTime.now());
        estadia.setAtiva(false);

        double valorFinal = calcularValor(estadia);
        estadia.setValor(valorFinal);

        Vaga vaga = estadia.getVaga();
        vaga.setOcupada(false);
        vagaRepository.save(vaga);

        return estadiaRepository.save(estadia);
    }

    public double consultarValor(Long estadiaId) {

        Estadia estadia = estadiaRepository.findById(estadiaId)
                .orElseThrow(() -> new RuntimeException("Estadia não encontrada"));

        if (!estadia.isAtiva()) {
            return estadia.getValor();
        }

        double valor = calcularValor(estadia);
        estadia.setValor(valor);
        estadiaRepository.save(estadia);

        return valor;
    }

    public List<Estadia> listarAtivas() {
        return estadiaRepository.findByAtivaTrue();
    }

    public Estadia buscarAtivaPorUsuario(String email) {
        Estadia estadia = estadiaRepository.findByAtivaTrueAndVeiculoUsuarioEmail(email)
                .orElse(null);

        if (estadia != null) {
            double valorAtual = calcularValor(estadia);
            estadia.setValor(valorAtual);
        }

        return estadia;
    }

    private double calcularValor(Estadia estadia) {
        LocalDateTime fim = estadia.isAtiva()
                ? LocalDateTime.now()
                : estadia.getSaida();

        long minutos = Duration.between(estadia.getEntrada(), fim).toMinutes();
        
        // Puxa o valor da hora configurado especificamente para este estacionamento
        double valorHora = estadia.getVaga().getEstacionamento().getValorHora();
        
        // Descobre o preço da fração (Ex: se a hora é R$ 10 e a fração é 30min, a fração custa R$ 5)
        double valorPorFracao = valorHora / (60.0 / MINUTOS_POR_FRACAO);

        long fracoes = (long) Math.ceil((double) minutos / MINUTOS_POR_FRACAO);
        
        // Garante que o motorista pague pelo menos a 1ª fração assim que entra
        if (fracoes == 0) {
            fracoes = 1;
        }

        return fracoes * valorPorFracao;
    }
}