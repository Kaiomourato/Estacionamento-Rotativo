package br.com.estacionamento.api.config;

import br.com.estacionamento.api.model.Estacionamento;
import br.com.estacionamento.api.repository.EstacionamentoRepository;
import br.com.estacionamento.api.repository.EstadiaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reproduz, em cima do H2, as duas formas de "schema drift" relatadas em
 * produção e confirma que o SchemaPatchRunner as corrige.
 */
@SpringBootTest
@DirtiesContext
class SchemaPatchRunnerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchemaPatchRunner schemaPatchRunner;

    @Autowired
    private EstacionamentoRepository estacionamentoRepository;

    @Autowired
    private EstadiaRepository estadiaRepository;

    // Bug #1: coluna "vagas_totais" (de uma versão antiga de Estacionamento)
    // ficou NOT NULL no banco mesmo depois de o campo ser removido da entidade.
    @Test
    void corrigeColunaVagasTotaisObsoletaQueQuebravaCadastroDeEstacionamento() {
        jdbcTemplate.execute("ALTER TABLE estacionamentos ADD COLUMN vagas_totais INTEGER NOT NULL");

        assertThatThrownBy(() -> estacionamentoRepository.saveAndFlush(
                new Estacionamento("Drift", "Endereco", -1.0, -1.0, 5.0)))
                .isInstanceOf(DataIntegrityViolationException.class);

        schemaPatchRunner.run();

        Estacionamento salvo = estacionamentoRepository.saveAndFlush(
                new Estacionamento("Corrigido", "Endereco", -1.0, -1.0, 5.0));
        assertThat(salvo.getId()).isNotNull();
    }

    // Bugs #4/#5/#7/#8: colunas novas de Estadia ("cancelada" e
    // "notificacao_cancelamento_lida") ainda não existiam na tabela, então
    // qualquer SELECT de Estadia (que sempre lista todas as colunas mapeadas)
    // quebrava com "column ... does not exist".
    @Test
    void corrigeColunasNovasDeEstadiaQueFaltavamNoBanco() {
        jdbcTemplate.execute("ALTER TABLE estadias DROP COLUMN cancelada");
        jdbcTemplate.execute("ALTER TABLE estadias DROP COLUMN notificacao_cancelamento_lida");

        assertThatThrownBy(() -> estadiaRepository.findByAtivaTrueAndVagaEstacionamentoId(1L))
                .isInstanceOf(DataAccessException.class);

        schemaPatchRunner.run();

        assertThatCode(() -> estadiaRepository.findByAtivaTrueAndVagaEstacionamentoId(1L))
                .doesNotThrowAnyException();
    }
}
