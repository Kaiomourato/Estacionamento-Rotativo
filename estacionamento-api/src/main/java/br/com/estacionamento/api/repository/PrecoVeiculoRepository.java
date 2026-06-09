package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.PrecoVeiculo;
import br.com.estacionamento.api.model.TipoVeiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrecoVeiculoRepository extends JpaRepository<PrecoVeiculo, Long> {

    List<PrecoVeiculo> findByEstacionamentoId(Long estacionamentoId);

    Optional<PrecoVeiculo> findByEstacionamentoIdAndTipoVeiculo(Long estacionamentoId, TipoVeiculo tipoVeiculo);
}
