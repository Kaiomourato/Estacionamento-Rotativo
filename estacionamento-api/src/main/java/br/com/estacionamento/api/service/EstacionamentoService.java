package br.com.estacionamento.api.service;

import br.com.estacionamento.api.dto.PrecoVeiculoDTO;
import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.model.PrecoVeiculo;
import br.com.estacionamento.api.model.TipoVeiculo;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.PrecoVeiculoRepository;
import br.com.estacionamento.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstacionamentoService {

    private final EstacionamentoRepository repository;
    private final UsuarioRepository usuarioRepository; // NOVO
    private final PrecoVeiculoRepository precoVeiculoRepository;

    public EstacionamentoService(EstacionamentoRepository repository, UsuarioRepository usuarioRepository,
                                  PrecoVeiculoRepository precoVeiculoRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.precoVeiculoRepository = precoVeiculoRepository;
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

    // NOVO: Busca apenas o estacionamento vinculado ao operador logado
    public Estacionamento buscarMeuEstacionamento(String email) {
        return buscarEstacionamentoDoOperador(email);
    }

    // NOVO: Atualiza nome e valor da hora (usado na nova aba)
    public Estacionamento atualizar(Long id, Estacionamento dados) {
        Estacionamento est = buscarPorId(id);
        est.setNome(dados.getNome());
        est.setValorHora(dados.getValorHora());
        return repository.save(est);
    }

    private Estacionamento buscarEstacionamentoDoOperador(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (usuario.getEstacionamento() == null) {
            throw new RuntimeException("Este operador não possui um estacionamento vinculado.");
        }
        return usuario.getEstacionamento();
    }

    // Lista os preços por tipo de veículo configurados para o estacionamento do operador
    public List<PrecoVeiculo> listarPrecos(String email) {
        Estacionamento estacionamento = buscarEstacionamentoDoOperador(email);
        return precoVeiculoRepository.findByEstacionamentoId(estacionamento.getId());
    }

    // Cria/atualiza (upsert) o valor da hora para cada tipo de veículo informado.
    // O dono do estacionamento decide se quer o mesmo valor para todos os tipos
    // (enviando o mesmo valorHora para cada um) ou valores diferentes por tipo.
    public List<PrecoVeiculo> atualizarPrecos(String email, List<PrecoVeiculoDTO> precos) {
        Estacionamento estacionamento = buscarEstacionamentoDoOperador(email);

        for (PrecoVeiculoDTO dto : precos) {
            if (dto.getTipoVeiculo() == null || dto.getValorHora() == null) {
                throw new RuntimeException("tipoVeiculo e valorHora são obrigatórios");
            }

            PrecoVeiculo preco = precoVeiculoRepository
                    .findByEstacionamentoIdAndTipoVeiculo(estacionamento.getId(), dto.getTipoVeiculo())
                    .orElseGet(() -> new PrecoVeiculo(estacionamento, dto.getTipoVeiculo(), dto.getValorHora()));

            preco.setValorHora(dto.getValorHora());
            precoVeiculoRepository.save(preco);
        }

        return precoVeiculoRepository.findByEstacionamentoId(estacionamento.getId());
    }

    // Resolve o valor da hora a cobrar para um tipo de veículo: usa o preço
    // específico cadastrado para o tipo, ou cai no valor padrão do estacionamento.
    public Double obterValorHora(Estacionamento estacionamento, TipoVeiculo tipoVeiculo) {
        if (tipoVeiculo != null) {
            return precoVeiculoRepository
                    .findByEstacionamentoIdAndTipoVeiculo(estacionamento.getId(), tipoVeiculo)
                    .map(PrecoVeiculo::getValorHora)
                    .orElse(estacionamento.getValorHora());
        }
        return estacionamento.getValorHora();
    }
}
