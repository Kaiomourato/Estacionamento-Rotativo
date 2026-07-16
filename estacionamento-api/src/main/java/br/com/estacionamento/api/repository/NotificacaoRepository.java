package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Notificacao;
import br.com.estacionamento.api.model.TipoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByDestinatarioEmailOrderByCriadoEmDesc(String email);

    long countByDestinatarioEmailAndLidaFalse(String email);

    Optional<Notificacao> findByIdAndDestinatarioEmail(Long id, String email);

    List<Notificacao> findByDestinatarioEmailAndLidaFalse(String email);

    boolean existsByEstadiaIdAndTipo(Long estadiaId, TipoNotificacao tipo);
}
