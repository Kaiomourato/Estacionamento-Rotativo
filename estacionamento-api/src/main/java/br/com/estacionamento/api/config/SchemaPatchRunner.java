package br.com.estacionamento.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Corrige divergências entre o banco e as entidades atuais que o
 * spring.jpa.hibernate.ddl-auto=update não resolve sozinho, pois ele nunca
 * remove colunas nem relaxa constraints de colunas que já existiam:
 *
 * - "estacionamentos.vagas_totais": coluna NOT NULL de uma versão antiga de
 *   Estacionamento (campo removido, virou getVagasTotais() calculado a
 *   partir de "vagas"). Sem essa correção, todo INSERT em /estacionamentos
 *   falha com "null value in column vagas_totais violates not-null
 *   constraint".
 * - "estadias.cancelada" / "estadias.notificacao_cancelamento_lida": colunas
 *   novas de Estadia; garante que existam mesmo que o ddl-auto ainda não
 *   tenha sido aplicado no banco em uso.
 *
 * Roda uma vez na subida da aplicação, depois que o Hibernate já aplicou o
 * próprio ddl-auto. Cada comando é independente e idempotente.
 */
@Component
public class SchemaPatchRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaPatchRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaPatchRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        executar("ALTER TABLE estacionamentos DROP COLUMN IF EXISTS vagas_totais");
        executar("ALTER TABLE estadias ADD COLUMN IF NOT EXISTS cancelada BOOLEAN NOT NULL DEFAULT false");
        executar("ALTER TABLE estadias ADD COLUMN IF NOT EXISTS notificacao_cancelamento_lida BOOLEAN NOT NULL DEFAULT false");
    }

    private void executar(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.warn("Falha ao aplicar patch de schema [{}]: {}", sql, e.getMessage());
        }
    }
}
