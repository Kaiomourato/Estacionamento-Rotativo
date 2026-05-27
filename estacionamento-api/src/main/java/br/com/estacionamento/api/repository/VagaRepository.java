package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VagaRepository extends JpaRepository<Vaga, Long> {
    Optional<Vaga> findByCodigo(String codigo);
    
    // No VagaRepository.java, mude a linha do findByEstacionamentoId para:
    List<Vaga> findByEstacionamentoIdAndAtivoTrue(Long estacionamentoId);
}