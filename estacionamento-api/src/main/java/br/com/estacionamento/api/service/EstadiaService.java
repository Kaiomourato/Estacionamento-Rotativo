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

    // valor base por fração (exemplo: a cada 30 minutos)
    private static final double VALOR_POR_FRACAO = 2.50;
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

    // 🚗 Iniciar estadia
    public Estadia iniciar(Long veiculoId, Long vagaId) {

        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));

        Vaga vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada"));

        if (vaga.isOcupada()) {
            throw new RuntimeException("Vaga já está ocupada");
        }

        // verifica se o veículo já possui estadia ativa
        boolean veiculoJaEstacionado = estadiaRepository.findByAtivaTrue()
                .stream()
                .anyMatch(e -> e.getVeiculo().getId().equals(veiculoId));

        if (veiculoJaEstacionado) {
            throw new RuntimeException("Veículo já possui uma estadia ativa");
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

    // 🧾 Finalizar estadia
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

    // 📊 Consultar valor atual
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

    // 📋 Listar estadias ativas
    public List<Estadia> listarAtivas() {
        return estadiaRepository.findByAtivaTrue();
    }

    // 💰 Regra de cálculo por fração
    private double calcularValor(Estadia estadia) {

        LocalDateTime fim = estadia.isAtiva()
                ? LocalDateTime.now()
                : estadia.getSaida();

        long minutos = Duration.between(estadia.getEntrada(), fim).toMinutes();

        long fracoes = (long) Math.ceil((double) minutos / MINUTOS_POR_FRACAO);

        return fracoes * VALOR_POR_FRACAO;
    }
}
