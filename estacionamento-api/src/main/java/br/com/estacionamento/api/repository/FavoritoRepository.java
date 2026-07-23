package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    List<Favorito> findByUsuarioEmail(String email);

    boolean existsByUsuarioEmailAndEstacionamentoId(String email, Long estacionamentoId);

    Optional<Favorito> findByUsuarioEmailAndEstacionamentoId(String email, Long estacionamentoId);
}
