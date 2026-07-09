package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);

    // NOVO (painel ADM): contagens usadas nos cards de usuários/administradores/operadores/motoristas
    long countByRole(String role);

    long countByRoleAndEstacionamentoIsNull(String role);

    long countByRoleAndEstacionamentoIsNotNull(String role);

    // NOVO (painel ADM): últimos usuários cadastrados (proxy de "mais recente" até existir criadoEm confiável)
    List<Usuario> findTop10ByOrderByIdDesc();

    // NOVO (painel ADM): gráfico de crescimento de usuários cadastrados
    List<Usuario> findByCriadoEmIsNotNullOrderByCriadoEmAsc();
}
