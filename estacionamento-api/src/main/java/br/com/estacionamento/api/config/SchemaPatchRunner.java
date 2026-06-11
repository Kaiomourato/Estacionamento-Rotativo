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
 *
 * Ao final, confere via information_schema se as colunas de Estadia
 * realmente existem e loga um ERROR explícito caso não existam (ex.: o
 * usuário do banco não tem permissão de ALTER TABLE), para que essa causa
 * não passe despercebida nos logs.
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

        verificarColunasEstadia();
    }

    private void executar(String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.info("Patch de schema aplicado com sucesso: {}", sql);
        } catch (Exception e) {
            // Loga em ERROR com a stacktrace completa: a causa real (ex.: permissão
            // negada, SQLState, etc.) precisa aparecer nos logs, não só a mensagem.
            log.error("Falha ao aplicar patch de schema [{}]", sql, e);
        }
    }

    // Confere se as colunas que o app precisa de fato existem após o patch.
    // O Hibernate sempre faz "SELECT *" mapeado nas entidades: se alguma dessas
    // colunas continuar faltando (ex.: usuário do banco sem permissão de ALTER),
    // toda rota que toca Estadia volta a quebrar com 500 e o motivo passaria
    // despercebido. Isso transforma essa falha silenciosa num log explícito.
    void verificarColunasEstadia() {
        for (String coluna : new String[] {"cancelada", "notificacao_cancelamento_lida"}) {
            try {
                Integer existe = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'estadias' AND column_name = ?",
                        Integer.class, coluna);
                if (existe == null || existe == 0) {
                    log.error("SCHEMA INCONSISTENTE: a coluna estadias.{} nao existe no banco. "
                            + "Toda rota que envolve Estadia (ativas, minha-ativa, historico, relatorio, etc.) "
                            + "vai falhar com 500 ate essa coluna ser criada. Execute manualmente no banco: "
                            + "ALTER TABLE estadias ADD COLUMN IF NOT EXISTS {} BOOLEAN NOT NULL DEFAULT false;",
                            coluna, coluna);
                }
            } catch (Exception e) {
                log.error("Falha ao verificar a coluna estadias.{}", coluna, e);
            }
        }
    }
}
