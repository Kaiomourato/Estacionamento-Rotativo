package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Vaga;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    List<Vaga> findByEstacionamentoIdAndAtivoTrue(Long estacionamentoId);

    // Código só precisa ser único dentro do mesmo estacionamento
    Optional<Vaga> findByEstacionamentoIdAndCodigoAndAtivoTrue(Long estacionamentoId, String codigo);

    // NOVO (painel ADM): listagem paginada e pesquisável de vagas de todos os estacionamentos
    @Query("SELECT v FROM Vaga v WHERE " +
            "(:estacionamentoId IS NULL OR v.estacionamento.id = :estacionamentoId) AND " +
            "(:busca IS NULL OR LOWER(v.codigo) LIKE LOWER(CONCAT('%', :busca, '%'))) " +
            "ORDER BY v.estacionamento.id, v.codigo")
    Page<Vaga> buscarParaAdmin(@Param("estacionamentoId") Long estacionamentoId, @Param("busca") String busca, Pageable pageable);
}
