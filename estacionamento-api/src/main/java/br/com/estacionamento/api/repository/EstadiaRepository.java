package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Estadia;
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
}