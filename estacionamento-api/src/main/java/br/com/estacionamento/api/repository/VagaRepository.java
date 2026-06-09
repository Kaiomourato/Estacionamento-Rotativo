package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    List<Vaga> findByEstacionamentoIdAndAtivoTrue(Long estacionamentoId);

    // Código só precisa ser único dentro do mesmo estacionamento
    Optional<Vaga> findByEstacionamentoIdAndCodigoAndAtivoTrue(Long estacionamentoId, String codigo);
}
