package br.com.estacionamento.api.service;

import br.com.estacionamento.api.dto.ResumoVagasDTO;
import br.com.estacionamento.api.dto.VagaRequestDTO;
import br.com.estacionamento.api.exception.RecursoNaoEncontradoException;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.TipoVeiculo;
import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.VagaRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class VagaService {

    private final VagaRepository repository;
    private final UsuarioRepository usuarioRepository;

    public VagaService(VagaRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    private Estacionamento buscarEstacionamentoDoOperador(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operador não encontrado"));

        if (usuario.getEstacionamento() == null) {
            throw new RuntimeException("Este operador não possui um estacionamento vinculado.");
        }

        return usuario.getEstacionamento();
    }

    // O estacionamento é sempre o do operador logado, evitando vagas "soltas"
    // e a antiga checagem global de código duplicado.
    public Vaga cadastrar(VagaRequestDTO dto, String email) {
        Estacionamento estacionamento = buscarEstacionamentoDoOperador(email);

        if (dto.getCodigo() == null || dto.getCodigo().isBlank()) {
            throw new RuntimeException("O código da vaga é obrigatório");
        }

        if (repository.findByEstacionamentoIdAndCodigoAndAtivoTrue(estacionamento.getId(), dto.getCodigo()).isPresent()) {
            throw new RuntimeException("Já existe uma vaga com esse código neste estacionamento");
        }

        Vaga vaga = new Vaga();
        vaga.setCodigo(dto.getCodigo());
        vaga.setTipoVeiculo(dto.getTipoVeiculo());
        vaga.setOcupada(false);
        vaga.setAtivo(true);
        vaga.setEstacionamento(estacionamento);

        return repository.save(vaga);
    }

    // Checagem de posse reaproveitada por toda operação que só o dono da vaga pode fazer
    // (editar, excluir, ocupar/liberar manualmente).
    private void verificarPropriedade(Vaga vaga, Estacionamento estacionamento) {
        if (!vaga.getEstacionamento().getId().equals(estacionamento.getId())) {
            throw new RuntimeException("Esta vaga não pertence ao seu estacionamento");
        }
    }

    public Vaga atualizar(Long id, VagaRequestDTO dto, String email) {
        Estacionamento estacionamento = buscarEstacionamentoDoOperador(email);
        Vaga vaga = buscarPorId(id);
        verificarPropriedade(vaga, estacionamento);

        if (dto.getCodigo() != null && !dto.getCodigo().isBlank() && !dto.getCodigo().equals(vaga.getCodigo())) {
            if (repository.findByEstacionamentoIdAndCodigoAndAtivoTrue(estacionamento.getId(), dto.getCodigo()).isPresent()) {
                throw new RuntimeException("Já existe uma vaga com esse código neste estacionamento");
            }
            vaga.setCodigo(dto.getCodigo());
        }

        vaga.setTipoVeiculo(dto.getTipoVeiculo());

        if (dto.getAtivo() != null && dto.getAtivo() != vaga.isAtivo()) {
            if (!dto.getAtivo() && vaga.isOcupada()) {
                throw new RuntimeException("Não é possível desativar uma vaga que está ocupada no momento.");
            }
            vaga.setAtivo(dto.getAtivo());
        }

        return repository.save(vaga);
    }

    // AGORA É ISOLADO: O Spring descobre o ID do estacionamento sozinho
    public List<Vaga> listarPorOperador(String email) {
        Estacionamento estacionamento = buscarEstacionamentoDoOperador(email);
        return repository.findByEstacionamentoIdAndAtivoTrue(estacionamento.getId());
    }

    // Resumo de vagas (total/livres/ocupadas) agrupado por tipo de veículo,
    // usado no lugar do antigo campo "vagasTotais"
    public List<ResumoVagasDTO> resumoPorOperador(String email) {
        Estacionamento estacionamento = buscarEstacionamentoDoOperador(email);
        List<Vaga> vagas = repository.findByEstacionamentoIdAndAtivoTrue(estacionamento.getId());

        Map<TipoVeiculo, long[]> contagens = new EnumMap<>(TipoVeiculo.class);
        long[] semTipo = new long[2]; // [total, ocupadas]

        for (Vaga vaga : vagas) {
            long[] contador;
            if (vaga.getTipoVeiculo() == null) {
                contador = semTipo;
            } else {
                contador = contagens.computeIfAbsent(vaga.getTipoVeiculo(), t -> new long[2]);
            }
            contador[0]++;
            if (vaga.isOcupada()) {
                contador[1]++;
            }
        }

        List<ResumoVagasDTO> resumo = new ArrayList<>();
        for (TipoVeiculo tipo : TipoVeiculo.values()) {
            long[] contador = contagens.get(tipo);
            if (contador != null) {
                resumo.add(new ResumoVagasDTO(tipo, contador[0], contador[0] - contador[1], contador[1]));
            }
        }
        if (semTipo[0] > 0) {
            resumo.add(new ResumoVagasDTO(null, semTipo[0], semTipo[0] - semTipo[1], semTipo[1]));
        }

        return resumo;
    }

    // Lista vagas ativas de um estacionamento — usado pelo motorista para reservar
    public List<Vaga> listarPorEstacionamento(Long estacionamentoId) {
        return repository.findByEstacionamentoIdAndAtivoTrue(estacionamentoId);
    }

    public Vaga buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada"));
    }

    // Antes não checava dono: qualquer usuário autenticado (de qualquer estacionamento)
    // conseguia ocupar/liberar a vaga de outro operador só sabendo o ID (IDOR).
    public Vaga ocuparVaga(Long id, String email) {
        Estacionamento estacionamento = buscarEstacionamentoDoOperador(email);
        Vaga vaga = buscarPorId(id);
        verificarPropriedade(vaga, estacionamento);

        if (vaga.isOcupada()) {
            throw new RuntimeException("Vaga já está ocupada");
        }
        vaga.setOcupada(true);
        return repository.save(vaga);
    }

    public Vaga liberarVaga(Long id, String email) {
        Estacionamento estacionamento = buscarEstacionamentoDoOperador(email);
        Vaga vaga = buscarPorId(id);
        verificarPropriedade(vaga, estacionamento);

        if (!vaga.isOcupada()) {
            throw new RuntimeException("Vaga já está livre");
        }
        vaga.setOcupada(false);
        return repository.save(vaga);
    }

    public void deletar(Long id, String email) {
        Estacionamento estacionamento = buscarEstacionamentoDoOperador(email);
        Vaga vaga = buscarPorId(id);

        if (!vaga.getEstacionamento().getId().equals(estacionamento.getId())) {
            throw new RecursoNaoEncontradoException("Vaga não encontrada");
        }

        if (vaga.isOcupada()) {
            throw new RuntimeException("Não é possível excluir uma vaga que está ocupada no momento.");
        }
        vaga.setAtivo(false);
        repository.save(vaga);
    }
}
