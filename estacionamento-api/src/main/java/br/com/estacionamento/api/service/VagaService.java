package br.com.estacionamento.api.service;

import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.VagaRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VagaService {

    private final VagaRepository repository;
    private final UsuarioRepository usuarioRepository;

    public VagaService(VagaRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    public Vaga cadastrar(Vaga vaga) {
        if (repository.findByCodigo(vaga.getCodigo()).isPresent()) {
            throw new RuntimeException("Já existe uma vaga com esse código");
        }
        vaga.setAtivo(true);
        return repository.save(vaga);
    }

    // AGORA É ISOLADO: O Spring descobre o ID do estacionamento sozinho
    public List<Vaga> listarPorOperador(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operador não encontrado"));
                
        if (usuario.getEstacionamento() == null) {
            throw new RuntimeException("Este operador não possui um estacionamento vinculado.");
        }
        
        return repository.findByEstacionamentoIdAndAtivoTrue(usuario.getEstacionamento().getId());
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

    public void deletar(Long id) {
        Vaga vaga = buscarPorId(id);
        if (vaga.isOcupada()) {
            throw new RuntimeException("Não é possível excluir uma vaga que está ocupada no momento.");
        }
        vaga.setAtivo(false); 
        repository.save(vaga);
    }
}