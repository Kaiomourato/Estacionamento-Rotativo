package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.LogAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LogAcessoRepository extends JpaRepository<LogAcesso, Long> {

    // NOVO (painel ADM): acessos registrados a partir de uma data — a contagem de
    // acessos/logins do período e o agrupamento por hora (pico/média) são feitos em
    // memória a partir desta lista, para respeitar também o limite superior do período.
    List<LogAcesso> findByDataHoraGreaterThanEqual(LocalDateTime desde);

    // NOVO (painel ADM): "usuários ativos" = e-mails distintos com acesso nas últimas 24h
    @Query("SELECT COUNT(DISTINCT l.usuarioEmail) FROM LogAcesso l WHERE l.usuarioEmail IS NOT NULL AND l.dataHora >= :desde")
    long countUsuariosAtivosDesde(@Param("desde") LocalDateTime desde);
}
