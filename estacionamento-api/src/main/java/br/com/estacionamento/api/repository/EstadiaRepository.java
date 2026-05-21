package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Estadia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EstadiaRepository extends JpaRepository<Estadia, Long> {

    List<Estadia> findByAtivaTrue();
    
    Optional<Estadia> findByAtivaTrueAndVeiculoUsuarioEmail(String email);
}