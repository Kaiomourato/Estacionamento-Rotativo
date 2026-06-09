package br.com.estacionamento.api.service;

import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.model.TipoVeiculo;
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
        if (vaga.getEstacionamento() == null || vaga.getEstacionamento().getId() == null) {
            throw new RuntimeException("Vaga precisa estar vinculada a um estacionamento.");
        }

        // Unicidade do código DENTRO do estacionamento — corrige o bug
        boolean codigoExiste = repository
            .findByCodigoAndEstacionamentoId(vaga.getCodigo(), vaga.getEstacionamento().getId())
            .isPresent();

        if (codigoExiste) {
            throw new RuntimeException("Já existe uma vaga com o código '"
                + vaga.getCodigo() + "' neste estacionamento.");
        }

        // Garante que slotsTotal seja definido pelo tipo
        if (vaga.getTipo() == null) vaga.setTipo(TipoVeiculo.CARRO);
        vaga.setSlotsTotal(Vaga.slotsDoTipo(vaga.getTipo()));
        vaga.setSlotsUsados(0);
        vaga.setAtivo(true);

        return repository.save(vaga);
    }

    public List<Vaga> listarPorOperador(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operador não encontrado"));

        if (usuario.getEstacionamento() == null) {
            throw new RuntimeException("Este operador não possui um estacionamento vinculado.");
        }

        return repository.findByEstacionamentoIdAndAtivoTrue(usuario.getEstacionamento().getId());
    }

    public List<Vaga> listarPorEstacionamento(Long estacionamentoId) {
        return repository.findByEstacionamentoIdAndAtivoTrue(estacionamentoId);
    }

    public Vaga buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada"));
    }

    public void deletar(Long id) {
        Vaga vaga = buscarPorId(id);
        if (vaga.isOcupada()) {
            throw new RuntimeException("Não é possível excluir uma vaga que está ocupada.");
        }
        vaga.setAtivo(false);
        repository.save(vaga);
    }
}
