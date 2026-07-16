package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);

    // NOVO (painel ADM): contagens usadas nos cards de usuários/administradores/operadores
    // (role no banco é sempre um de "ADMIN", "OPERADOR" ou "USER" — motorista = role "USER")
    long countByRole(String role);

    // NOVO (painel ADM): últimos usuários cadastrados (proxy de "mais recente" até existir criadoEm confiável)
    List<Usuario> findTop10ByOrderByIdDesc();

    // NOVO (painel ADM): gráfico de crescimento de usuários cadastrados
    List<Usuario> findByCriadoEmIsNotNullOrderByCriadoEmAsc();

    // Operadores vinculados a um estacionamento (para notificar todos ao receber uma nova reserva)
    List<Usuario> findByEstacionamentoIdAndRole(Long estacionamentoId, String role);

    // NOVO (painel ADM): listagem paginada e pesquisável para as páginas Usuários/Operadores
    @Query("SELECT u FROM Usuario u WHERE " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:busca IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :busca, '%'))) " +
            "ORDER BY u.id DESC")
    Page<Usuario> buscarParaAdmin(@Param("role") String role, @Param("busca") String busca, Pageable pageable);
}
