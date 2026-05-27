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

    public Vaga cadastrar(Vaga vaga) {
        if (repository.findByCodigo(vaga.getCodigo()).isPresent()) {
            throw new RuntimeException("Já existe uma vaga com esse código");
        }
        return repository.save(vaga);
    }

    public Vaga buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada"));
    }

    public Vaga ocuparVaga(Long id) {
        Vaga vaga = buscarPorId(id);
        if (vaga.isOcupada()) {
            throw new RuntimeException("Vaga já está ocupada");
        }
        vaga.setOcupada(true);
        return repository.save(vaga);
    }

    public Vaga liberarVaga(Long id) {
        Vaga vaga = buscarPorId(id);
        if (!vaga.isOcupada()) {
            throw new RuntimeException("Vaga já está livre");
        }
        vaga.setOcupada(false);
        return repository.save(vaga);
    }

    // No VagaService.java, ajuste a listagem para usar o método novo:
    public List<Vaga> listarPorEstacionamento(Long estacionamentoId) {
        if (estacionamentoId != null) {
            return repository.findByEstacionamentoIdAndAtivoTrue(estacionamentoId);
        }
        return repository.findAll(); // admin pode querer ver tudo
    }

    // No VagaService.java, modifique a função deletar para o Soft Delete:
    public void deletar(Long id) {
        Vaga vaga = buscarPorId(id);
        if (vaga.isOcupada()) {
            throw new RuntimeException("Não é possível excluir uma vaga que está ocupada no momento.");
        }
        vaga.setAtivo(false); // Apenas desativa em vez de apagar do banco!
        repository.save(vaga);
    }
}