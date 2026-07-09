package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Estadia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EstadiaRepository extends JpaRepository<Estadia, Long> {

    // Método antigo (Global) - Pode manter por segurança se usar em outro lugar
    List<Estadia> findByAtivaTrue();

    // NOVO: Busca estadias ativas vinculadas apenas ao estacionamento alvo
    List<Estadia> findByAtivaTrueAndVagaEstacionamentoId(Long estacionamentoId);

    // Método usado pelo Motorista para ver o bilhete dele
    Optional<Estadia> findByAtivaTrueAndVeiculoUsuarioEmail(String email);

    // Busca a estadia ativa do motorista, ou uma reserva recém-cancelada que ele
    // ainda não foi notificado (para o polling de /minha-ativa detectar o cancelamento)
    @Query("SELECT e FROM Estadia e WHERE e.veiculo.usuario.email = :email "
            + "AND (e.ativa = true OR (e.cancelada = true AND e.notificacaoCancelamentoLida = false)) "
            + "ORDER BY e.id DESC")
    List<Estadia> findAtivaOuCanceladaNaoLidaByVeiculoUsuarioEmail(@Param("email") String email);

    // Histórico do motorista: estadias já encerradas (finalizadas ou canceladas), mais recentes primeiro
    List<Estadia> findByVeiculoUsuarioEmailAndAtivaFalseOrderByIdDesc(String email);

    // Busca a reserva pendente pelo código de check-in
    Optional<Estadia> findByCodigoAndPendenteTrue(String codigo);

    // Usado para garantir que o código gerado para a reserva seja único
    boolean existsByCodigoAndPendenteTrue(String codigo);

    // Estadias em andamento (já em uso, ainda sem saída) de um estacionamento
    List<Estadia> findByVagaEstacionamentoIdAndAtivaTrueAndPendenteFalse(Long estacionamentoId);

    // Estadias finalizadas (com saída registrada) a partir de uma data
    List<Estadia> findByVagaEstacionamentoIdAndSaidaIsNotNullAndSaidaGreaterThanEqual(Long estacionamentoId, LocalDateTime desde);

    // Estadias com entrada registrada a partir de uma data
    List<Estadia> findByVagaEstacionamentoIdAndEntradaIsNotNullAndEntradaGreaterThanEqual(Long estacionamentoId, LocalDateTime desde);

    // Histórico do operador: estadias encerradas (finalizadas ou canceladas) do seu estacionamento, mais recentes primeiro
    List<Estadia> findByVagaEstacionamentoIdAndAtivaFalseOrderByIdDesc(Long estacionamentoId);

    // NOVO (painel ADM): todas as estadias criadas a partir de uma data, em QUALQUER estacionamento
    List<Estadia> findByCriadoEmGreaterThanEqual(LocalDateTime desde);

    // NOVO (painel ADM): estadias finalizadas a partir de uma data, em QUALQUER estacionamento
    List<Estadia> findBySaidaIsNotNullAndSaidaGreaterThanEqual(LocalDateTime desde);

    // NOVO (painel ADM): estadias em andamento agora, em QUALQUER estacionamento
    List<Estadia> findByAtivaTrueAndPendenteFalse();

    // NOVO (painel ADM): estadias com entrada registrada a partir de uma data, em QUALQUER estacionamento
    List<Estadia> findByEntradaIsNotNullAndEntradaGreaterThanEqual(LocalDateTime desde);

    // NOVO (painel ADM): últimos check-ins e check-outs (tabelas do dashboard), globais ou de um estacionamento
    List<Estadia> findByEntradaIsNotNullOrderByEntradaDesc(Pageable pageable);

    List<Estadia> findBySaidaIsNotNullOrderBySaidaDesc(Pageable pageable);

    List<Estadia> findByVagaEstacionamentoIdAndEntradaIsNotNullOrderByEntradaDesc(Long estacionamentoId, Pageable pageable);

    List<Estadia> findByVagaEstacionamentoIdAndSaidaIsNotNullOrderBySaidaDesc(Long estacionamentoId, Pageable pageable);

    // NOVO (painel ADM): ranking de estacionamentos — total de estadias e faturamento
    // histórico, numa única query agrupada (antes eram duas consultas separadas).
    // Cada linha: [estacionamentoId, totalEstadias, faturamentoTotal]
    @Query("SELECT e.vaga.estacionamento.id, COUNT(e), COALESCE(SUM(e.valor), 0) FROM Estadia e GROUP BY e.vaga.estacionamento.id")
    List<Object[]> calcularEstatisticasPorEstacionamento();

    // NOVO (painel ADM): página "Pagamentos" — estadias finalizadas (paga = valor calculado
    // no check-out), com filtro opcional por estacionamento e por período
    @Query("SELECT e FROM Estadia e WHERE e.saida IS NOT NULL AND e.valor IS NOT NULL AND " +
            "(:estacionamentoId IS NULL OR e.vaga.estacionamento.id = :estacionamentoId) AND " +
            "(:desde IS NULL OR e.saida >= :desde) AND (:ate IS NULL OR e.saida < :ate) " +
            "ORDER BY e.saida DESC")
    Page<Estadia> buscarPagamentos(@Param("estacionamentoId") Long estacionamentoId,
                                    @Param("desde") LocalDateTime desde, @Param("ate") LocalDateTime ate,
                                    Pageable pageable);
}