package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    Optional<Veiculo> findByPlaca(String placa);
    
    // NOVO: Busca lista de veículos pelo e-mail do dono
    List<Veiculo> findByUsuarioEmail(String email);

    boolean existsByPlaca(String placa);
}