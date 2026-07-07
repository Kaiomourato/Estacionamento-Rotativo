package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.LogAcesso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAcessoRepository extends JpaRepository<LogAcesso, Long> {
}
