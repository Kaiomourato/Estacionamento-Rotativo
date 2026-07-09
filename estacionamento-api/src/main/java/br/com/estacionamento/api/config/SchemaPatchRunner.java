package br.com.estacionamento.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

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
 * - colunas novas de Estadia ("pendente", "codigo", "criado_em",
 *   "previsao_chegada", "cancelada", "notificacao_cancelamento_lida"):
 *   garante que existam mesmo que o ddl-auto ainda não tenha sido aplicado
 *   no banco em uso (ex.: usuário do banco sem permissão de ALTER TABLE em
 *   tabelas pré-existentes).
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

    // Colunas que a entidade Estadia espera encontrar na tabela "estadias",
    // mapeadas para o tipo/constraint usado no ADD COLUMN IF NOT EXISTS.
    private static final Map<String, String> COLUNAS_ESTADIA = new LinkedHashMap<>();
    static {
        COLUNAS_ESTADIA.put("pendente", "BOOLEAN NOT NULL DEFAULT false");
        COLUNAS_ESTADIA.put("codigo", "VARCHAR(6)");
        COLUNAS_ESTADIA.put("criado_em", "TIMESTAMP");
        COLUNAS_ESTADIA.put("previsao_chegada", "TIMESTAMP");
        COLUNAS_ESTADIA.put("cancelada", "BOOLEAN NOT NULL DEFAULT false");
        COLUNAS_ESTADIA.put("notificacao_cancelamento_lida", "BOOLEAN NOT NULL DEFAULT false");
    }

    private final JdbcTemplate jdbcTemplate;

    public SchemaPatchRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        executar("ALTER TABLE estacionamentos DROP COLUMN IF EXISTS vagas_totais");

        COLUNAS_ESTADIA.forEach((coluna, tipo) ->
                executar("ALTER TABLE estadias ADD COLUMN IF NOT EXISTS " + coluna + " " + tipo));

        // NOVO (painel ADM): usada no gráfico de crescimento de usuários cadastrados.
        // Nula para contas criadas antes desta coluna existir.
        executar("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS criado_em TIMESTAMP");

        // Reservas pendentes (Estadia.pendente=true) ainda não têm horário de
        // entrada real - ele só é definido no check-in. A coluna "entrada"
        // veio do schema legado como NOT NULL, o que quebrava POST
        // /estadias/reservar com 500 (constraint violation).
        executar("ALTER TABLE estadias ALTER COLUMN entrada DROP NOT NULL");

        // Corrige vagas "fantasma": deixadas como ocupada=true por reservas que
        // falharam antes da correção acima, sem nenhuma estadia ativa associada.
        executar("UPDATE vagas SET ocupada = false WHERE ocupada = true "
                + "AND id NOT IN (SELECT vaga_id FROM estadias WHERE ativa = true)");

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
        for (Map.Entry<String, String> entry : COLUNAS_ESTADIA.entrySet()) {
            String coluna = entry.getKey();
            String tipo = entry.getValue();
            try {
                // LOWER() em ambos os lados: H2 guarda nomes nao-quotados em
                // MAIUSCULO (ESTADIAS/PENDENTE) enquanto o Postgres guarda em
                // minusculo, entao a comparacao precisa ser case-insensitive
                // para funcionar nos dois.
                Integer existe = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'estadias' AND LOWER(column_name) = LOWER(?)",
                        Integer.class, coluna);
                if (existe == null || existe == 0) {
                    log.error("SCHEMA INCONSISTENTE: a coluna estadias.{} nao existe no banco. "
                            + "Toda rota que envolve Estadia (ativas, minha-ativa, historico, relatorio, etc.) "
                            + "vai falhar com 500 ate essa coluna ser criada. Execute manualmente no banco: "
                            + "ALTER TABLE estadias ADD COLUMN IF NOT EXISTS {} {};",
                            coluna, coluna, tipo);
                }
            } catch (Exception e) {
                log.error("Falha ao verificar a coluna estadias.{}", coluna, e);
            }
        }
    }
}
