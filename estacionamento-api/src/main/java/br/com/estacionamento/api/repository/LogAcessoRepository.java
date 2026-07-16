package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.LogAcesso;
import br.com.estacionamento.api.model.TipoEventoLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // NOVO (auditoria): busca paginada com todos os filtros da tela de Auditoria.
    // Usada tanto para a listagem (com Pageable normal) quanto para a exportação em
    // CSV (chamada com Pageable.unpaged(), sem LIMIT/OFFSET).
    // CAST(:usuarioEmail/:rota AS string): sem isso, o Postgres falha com "function
    // lower(bytea) does not exist" sempre que o parâmetro é null (sem valor pra inferir
    // o tipo do bind do CONCAT/LOWER, ele resolve como bytea). O CAST fixa o tipo
    // independente do valor, então o "IS NULL" continua funcionando normalmente.
    @Query("SELECT l FROM LogAcesso l WHERE " +
            "(:usuarioEmail IS NULL OR LOWER(l.usuarioEmail) LIKE LOWER(CONCAT('%', CAST(:usuarioEmail AS string), '%'))) AND " +
            "(:desde IS NULL OR l.dataHora >= :desde) AND " +
            "(:ate IS NULL OR l.dataHora < :ate) AND " +
            "(:tipoEvento IS NULL OR l.tipoEvento = :tipoEvento) AND " +
            "(:rota IS NULL OR LOWER(l.rota) LIKE LOWER(CONCAT('%', CAST(:rota AS string), '%'))) AND " +
            "(:role IS NULL OR l.role = :role) AND " +
            "(:status IS NULL OR l.status = :status) " +
            "ORDER BY l.dataHora DESC")
    Page<LogAcesso> buscarComFiltros(@Param("usuarioEmail") String usuarioEmail,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("ate") LocalDateTime ate,
                                      @Param("tipoEvento") TipoEventoLog tipoEvento,
                                      @Param("rota") String rota,
                                      @Param("role") String role,
                                      @Param("status") Integer status,
                                      Pageable pageable);
}
