package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Vaga;
import br.com.estacionamento.api.model.TipoVeiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    // Unicidade por código DENTRO do estacionamento (corrige o bug global)
    Optional<Vaga> findByCodigoAndEstacionamentoId(String codigo, Long estacionamentoId);

    List<Vaga> findByEstacionamentoIdAndAtivoTrue(Long estacionamentoId);

    List<Vaga> findByEstacionamentoIdAndAtivoTrueAndTipo(Long estacionamentoId, TipoVeiculo tipo);
}
