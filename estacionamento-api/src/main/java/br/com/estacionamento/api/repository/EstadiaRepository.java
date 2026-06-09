package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Estadia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EstadiaRepository extends JpaRepository<Estadia, Long> {

    List<Estadia> findByAtivaTrue();

    List<Estadia> findByAtivaTrueAndVagaEstacionamentoId(Long estacionamentoId);

    Optional<Estadia> findByAtivaTrueAndVeiculoUsuarioEmail(String email);

    // Histórico do operador — estadias encerradas do seu estacionamento
    List<Estadia> findByAtivaFalseAndVagaEstacionamentoIdOrderByEntradaDesc(Long estacionamentoId);

    // Histórico do motorista — estadias encerradas dos seus veículos
    List<Estadia> findByAtivaFalseAndVeiculoUsuarioEmailOrderByEntradaDesc(String email);

    // Para validar check-in pelo código de reserva
    Optional<Estadia> findByCodigo(String codigo);
}
