package br.com.estacionamento.api.service;

import br.com.estacionamento.api.model.Veiculo;
import br.com.estacionamento.api.repository.VeiculoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VeiculoService {

    private final VeiculoRepository repository;

    public VeiculoService(VeiculoRepository repository) {
        this.repository = repository;
    }

    public Veiculo cadastrar(Veiculo veiculo) {
        if (repository.existsByPlaca(veiculo.getPlaca())) {
            throw new RuntimeException("Já existe um veículo com essa placa");
        }
        return repository.save(veiculo);
    }

    public List<Veiculo> listarTodos() {
        return repository.findAll();
    }

    public Veiculo buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));
    }

    public void desativar(Long id) {
        Veiculo veiculo = buscarPorId(id);
        veiculo.setAtivo(false);
        repository.save(veiculo);
    }
}
