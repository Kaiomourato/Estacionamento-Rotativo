package br.com.estacionamento.api.service;

import br.com.estacionamento.api.model.*;
import br.com.estacionamento.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class EstadiaService {

    private final EstadiaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final VeiculoRepository veiculoRepository;
    private final VagaRepository vagaRepository;

    public EstadiaService(EstadiaRepository repository, UsuarioRepository usuarioRepository,
                          VeiculoRepository veiculoRepository, VagaRepository vagaRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.veiculoRepository = veiculoRepository;
        this.vagaRepository = vagaRepository;
    }

    // ── Operador: lista ativas do seu estacionamento ──────────────────────────
    public List<Estadia> listarAtivasPorOperador(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operador não encontrado"));
        if (usuario.getEstacionamento() == null)
            throw new RuntimeException("Este operador não possui estacionamento vinculado.");
        return repository.findByAtivaTrueAndVagaEstacionamentoId(usuario.getEstacionamento().getId());
    }

    // ── Operador: histórico encerrado ─────────────────────────────────────────
    public List<Estadia> historicoDoOperador(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operador não encontrado"));
        if (usuario.getEstacionamento() == null)
            throw new RuntimeException("Este operador não possui estacionamento vinculado.");
        return repository.findByAtivaFalseAndVagaEstacionamentoIdOrderByEntradaDesc(
                usuario.getEstacionamento().getId());
    }

    // ── Motorista: estadia ativa ──────────────────────────────────────────────
    public Estadia buscarEstadiaAtivaDoMotorista(String email) {
        return repository.findByAtivaTrueAndVeiculoUsuarioEmail(email)
                .orElseThrow(() -> new RuntimeException("Nenhuma estadia ativa encontrada."));
    }

    // ── Motorista: histórico encerrado ────────────────────────────────────────
    public List<Estadia> historicoDoMotorista(String email) {
        return repository.findByAtivaFalseAndVeiculoUsuarioEmailOrderByEntradaDesc(email);
    }

    // ── Entrada manual pelo operador ──────────────────────────────────────────
    @Transactional
    public Estadia registrarEntrada(String placa, Long vagaId) {
        Veiculo veiculo = veiculoRepository.findByPlaca(placa)
                .orElseThrow(() -> new RuntimeException("Veículo com placa '" + placa + "' não encontrado. Peça ao motorista para cadastrar o veículo no app."));

        Vaga vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada"));

        if (!vaga.aceitaVeiculo(veiculo.getTipo())) {
            if (vaga.isOcupada()) {
                throw new RuntimeException("Esta vaga está cheia.");
            }
            throw new RuntimeException("Esta vaga (tipo " + vaga.getTipo() + ") não aceita veículos do tipo " + veiculo.getTipo() + ".");
        }

        vaga.ocupar(veiculo.getTipo());
        vagaRepository.save(vaga);

        Estadia estadia = new Estadia();
        estadia.setVeiculo(veiculo);
        estadia.setVaga(vaga);
        estadia.setEntrada(LocalDateTime.now());
        estadia.setAtiva(true);
        estadia.setPendente(false);

        return repository.save(estadia);
    }

    // ── Motorista: reserva de vaga (gera código) ──────────────────────────────
    @Transactional
    public Estadia reservar(String emailMotorista, Long vagaId) {
        Usuario motorista = usuarioRepository.findByEmail(emailMotorista)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Busca o primeiro veículo ativo do motorista
        List<Veiculo> veiculos = veiculoRepository.findByUsuarioEmail(emailMotorista);
        if (veiculos.isEmpty())
            throw new RuntimeException("Cadastre um veículo antes de reservar.");

        Vaga vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada"));

        // Usa o primeiro veículo por padrão; o check-in pode confirmar o correto
        Veiculo veiculo = veiculos.get(0);

        if (!vaga.aceitaVeiculo(veiculo.getTipo())) {
            throw new RuntimeException("Esta vaga não aceita o tipo do seu veículo.");
        }

        // Gera código alfanumérico de 6 caracteres
        String codigo = UUID.randomUUID().toString()
                .replaceAll("[^A-Z0-9]", "")
                .substring(0, 6)
                .toUpperCase();

        // Reserva ocupa os slots mas fica pendente até o check-in
        vaga.ocupar(veiculo.getTipo());
        vagaRepository.save(vaga);

        Estadia estadia = new Estadia();
        estadia.setVeiculo(veiculo);
        estadia.setVaga(vaga);
        estadia.setEntrada(LocalDateTime.now()); // será resetado no check-in
        estadia.setAtiva(true);
        estadia.setPendente(true);
        estadia.setCodigo(codigo);

        return repository.save(estadia);
    }

    // ── Operador: confirma check-in pelo código ───────────────────────────────
    @Transactional
    public Estadia confirmarCheckin(String codigo) {
        Estadia estadia = repository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Código inválido ou expirado."));

        if (!estadia.isPendente())
            throw new RuntimeException("Este código já foi utilizado.");

        if (!estadia.isAtiva())
            throw new RuntimeException("Esta reserva foi cancelada.");

        // Confirma: zera o tempo de entrada para contar a partir de agora
        estadia.setEntrada(LocalDateTime.now());
        estadia.setPendente(false);

        return repository.save(estadia);
    }

    // ── Encerrar estadia (operador) ───────────────────────────────────────────
    @Transactional
    public Estadia finalizarEstadia(Long id) {
        Estadia estadia = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estadia não encontrada"));

        if (!estadia.isAtiva())
            throw new RuntimeException("Esta estadia já está encerrada.");

        LocalDateTime saida = LocalDateTime.now();
        estadia.setSaida(saida);
        estadia.setAtiva(false);

        Vaga vaga = estadia.getVaga();
        vaga.liberar(estadia.getVeiculo().getTipo());
        vagaRepository.save(vaga);

        // Cálculo proporcional (fração de hora)
        long minutos = ChronoUnit.MINUTES.between(estadia.getEntrada(), saida);
        double horas = Math.max(minutos / 60.0, 0.0);

        // Busca o valor/hora correto pelo tipo do veículo
        Double valorHora = vaga.getEstacionamento()
                .getValorHoraPorTipo(estadia.getVeiculo().getTipo());

        estadia.setValor(Math.round(horas * valorHora * 100.0) / 100.0);

        return repository.save(estadia);
    }
}
