package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Estadia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EstadiaRepository extends JpaRepository<Estadia, Long> {
    
    // Método antigo (Global) - Pode manter por segurança se usar em outro lugar
    List<Estadia> findByAtivaTrue();
    
    // NOVO: Busca estadias ativas vinculadas apenas ao estacionamento alvo
    List<Estadia> findByAtivaTrueAndVagaEstacionamentoId(Long estacionamentoId);
    
    // Método usado pelo Motorista para ver o bilhete dele
    Optional<Estadia> findByAtivaTrueAndVeiculoUsuarioEmail(String email);
}