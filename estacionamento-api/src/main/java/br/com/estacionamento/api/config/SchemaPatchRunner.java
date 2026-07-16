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
 * - "usuarios.email" como bytea em vez de texto: o ddl-auto=update nunca corrige
 *   o tipo de uma coluna que já existe. Toda busca em /admin/usuarios que usa
 *   lower(email) quebrava com "ERROR: function lower(bytea) does not exist"
 *   (500 sempre, em toda chamada — não intermitente; confirmado via log [DIAG]
 *   em AdminController/AdminDashboardService). Os dados gravados nessa coluna
 *   sempre foram os bytes UTF-8 do e-mail normal, então corrigir o tipo não
 *   perde nenhum dado existente.
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

        // NOVO (auditoria): campos ricos de contexto para cada requisição registrada.
        // Nulos para os logs já existentes antes desta coluna existir.
        executar("ALTER TABLE logs_acesso ADD COLUMN IF NOT EXISTS role VARCHAR(30)");
        executar("ALTER TABLE logs_acesso ADD COLUMN IF NOT EXISTS ip VARCHAR(64)");
        executar("ALTER TABLE logs_acesso ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512)");
        executar("ALTER TABLE logs_acesso ADD COLUMN IF NOT EXISTS navegador VARCHAR(50)");
        executar("ALTER TABLE logs_acesso ADD COLUMN IF NOT EXISTS sistema_operacional VARCHAR(50)");
        executar("ALTER TABLE logs_acesso ADD COLUMN IF NOT EXISTS tipo_evento VARCHAR(50)");
        executar("ALTER TABLE logs_acesso ADD COLUMN IF NOT EXISTS descricao VARCHAR(255)");

        // Reservas pendentes (Estadia.pendente=true) ainda não têm horário de
        // entrada real - ele só é definido no check-in. A coluna "entrada"
        // veio do schema legado como NOT NULL, o que quebrava POST
        // /estadias/reservar com 500 (constraint violation).
        executar("ALTER TABLE estadias ALTER COLUMN entrada DROP NOT NULL");

        // O cadastro público (Registro.jsx) já enviava latitude/longitude nulas quando a
        // geolocalização do navegador falhava/era negada, mas a coluna era NOT NULL —
        // isso quebrava POST /estacionamentos com 500 (constraint violation) nesse caso.
        executar("ALTER TABLE estacionamentos ALTER COLUMN latitude DROP NOT NULL");
        executar("ALTER TABLE estacionamentos ALTER COLUMN longitude DROP NOT NULL");

        // Corrige vagas "fantasma": deixadas como ocupada=true por reservas que
        // falharam antes da correção acima, sem nenhuma estadia ativa associada.
        executar("UPDATE vagas SET ocupada = false WHERE ocupada = true "
                + "AND id NOT IN (SELECT vaga_id FROM estadias WHERE ativa = true)");

        // Rede de segurança para a tabela "notificacoes" (entidade Notificacao): o
        // ddl-auto=update do Hibernate já deveria criá-la, mas caso o usuário do banco em
        // produção não tenha permissão para CREATE TABLE, essa falha ficaria silenciosa até
        // o primeiro INSERT em runtime. IF NOT EXISTS torna esse comando um no-op nos casos
        // normais (tabela já criada pelo Hibernate).
        executar("CREATE TABLE IF NOT EXISTS notificacoes (" +
                "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
                "destinatario_id BIGINT NOT NULL, " +
                "tipo VARCHAR(50) NOT NULL, " +
                "titulo VARCHAR(255) NOT NULL, " +
                "mensagem VARCHAR(1000) NOT NULL, " +
                "lida BOOLEAN NOT NULL DEFAULT false, " +
                "criado_em TIMESTAMP NOT NULL, " +
                "estadia_id BIGINT)");

        corrigirTipoColunaEmailUsuarios();

        verificarColunasEstadia();
        verificarTabelaNotificacoes();
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

    // Corrige usuarios.email quando a coluna ficou como bytea em vez de texto (ver
    // Javadoc da classe). Só altera o tipo se o information_schema confirmar que a
    // coluna está mesmo como bytea — em bancos já corretos (H2 dos testes, Postgres
    // já corrigido) isso é um no-op silencioso.
    private void corrigirTipoColunaEmailUsuarios() {
        try {
            String tipo = jdbcTemplate.queryForObject(
                    "SELECT data_type FROM information_schema.columns WHERE LOWER(table_name) = 'usuarios' AND LOWER(column_name) = 'email'",
                    String.class);
            if ("bytea".equalsIgnoreCase(tipo)) {
                log.warn("SCHEMA INCONSISTENTE: usuarios.email esta como bytea em vez de texto. "
                        + "Corrigindo o tipo da coluna (dados existentes sao reinterpretados como UTF-8, sem perda).");
                executar("ALTER TABLE usuarios ALTER COLUMN email TYPE VARCHAR(255) USING convert_from(email, 'UTF8')");
            }
        } catch (Exception e) {
            log.error("Falha ao verificar/corrigir o tipo da coluna usuarios.email", e);
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

    // Mesma ideia de verificarColunasEstadia(), mas para a existência da própria tabela
    // "notificacoes": se o CREATE TABLE IF NOT EXISTS acima falhar por permissão, toda rota
    // de /notificacoes (e a criação de notificações a partir de EstadiaService/Scheduler)
    // vai quebrar com 500 sem explicação nos logs.
    void verificarTabelaNotificacoes() {
        try {
            Integer existe = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = 'notificacoes'",
                    Integer.class);
            if (existe == null || existe == 0) {
                log.error("SCHEMA INCONSISTENTE: a tabela notificacoes nao existe no banco. "
                        + "Toda rota de /notificacoes e a criacao de notificacoes a partir de "
                        + "EstadiaService/NotificacaoScheduler vao falhar com 500 ate essa tabela ser criada. "
                        + "Execute manualmente no banco o CREATE TABLE definido em SchemaPatchRunner.");
            }
        } catch (Exception e) {
            log.error("Falha ao verificar a tabela notificacoes", e);
        }
    }
}
