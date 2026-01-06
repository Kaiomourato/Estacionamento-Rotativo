package br.com.estacionamento.api.service;

import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.repository.VagaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VagaService {

    private final VagaRepository repository;

    public VagaService(VagaRepository repository) {
        this.repository = repository;
    }

    // Cadastrar uma nova vaga
    public Vaga cadastrar(Vaga vaga) {
        if (repository.findByCodigo(vaga.getCodigo()).isPresent()) {
            throw new RuntimeException("Já existe uma vaga com esse código");
        }
        return repository.save(vaga);
    }

    // Listar todas as vagas
    public List<Vaga> listarTodos() {
        return repository.findAll();
    }

    // Buscar vaga por ID
    public Vaga buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada"));
    }

    // Ocupar uma vaga
    public Vaga ocuparVaga(Long id) {
        Vaga vaga = buscarPorId(id);
        if (vaga.isOcupada()) {
            throw new RuntimeException("Vaga já está ocupada");
        }
        vaga.setOcupada(true);
        return repository.save(vaga);
    }

    // Liberar uma vaga
    public Vaga liberarVaga(Long id) {
        Vaga vaga = buscarPorId(id);
        if (!vaga.isOcupada()) {
            throw new RuntimeException("Vaga já está livre");
        }
        vaga.setOcupada(false);
        return repository.save(vaga);
    }
}
