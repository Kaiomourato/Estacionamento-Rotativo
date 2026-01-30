package br.com.estacionamento.api.service;

import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstacionamentoService {

    private final EstacionamentoRepository repository;

    public EstacionamentoService(EstacionamentoRepository repository) {
        this.repository = repository;
    }

    public List<Estacionamento> listarTodos() {
        return repository.findAll();
    }

    public List<Estacionamento> buscarProximos(Double lat, Double lng, Double raio) {
        return repository.findProximos(lat, lng, raio);
    }

    public Estacionamento salvar(Estacionamento estacionamento) {
        return repository.save(estacionamento);
    }

    public Estacionamento buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Estacionamento não encontrado"));
    }
}
