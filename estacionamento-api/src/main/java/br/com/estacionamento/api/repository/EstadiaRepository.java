package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Estadia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstadiaRepository extends JpaRepository<Estadia, Long> {
    List<Estadia> findByAtivaTrue();
}
