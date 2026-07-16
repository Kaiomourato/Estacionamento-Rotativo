package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.DispositivoFcm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DispositivoFcmRepository extends JpaRepository<DispositivoFcm, Long> {

    List<DispositivoFcm> findByUsuarioEmail(String email);

    Optional<DispositivoFcm> findByToken(String token);

    void deleteByToken(String token);
}
