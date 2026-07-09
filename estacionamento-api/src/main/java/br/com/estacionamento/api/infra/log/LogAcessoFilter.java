package br.com.estacionamento.api.infra.log;

import br.com.estacionamento.api.model.LogAcesso;
import br.com.estacionamento.api.model.TipoEventoLog;
import br.com.estacionamento.api.model.Usuario;
import br.com.estacionamento.api.repository.LogAcessoRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

// Registra toda requisição como um evento de auditoria: quem fez, de onde (IP,
// navegador, SO), o quê (rota/método), quando e o resultado (status HTTP),
// classificado num tipo de evento semântico (login, check-in, erro, etc.).
@Component
public class LogAcessoFilter extends OncePerRequestFilter {

    @Autowired
    private LogAcessoRepository logAcessoRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        String metodo = request.getMethod();
        String rota = request.getRequestURI();
        int status = response.getStatus();
        String userAgent = request.getHeader("User-Agent");

        TipoEventoLog tipoEvento = classificarEvento(metodo, rota, status);

        LogAcesso log = new LogAcesso(recoverUsuarioEmail(), metodo, rota, status, LocalDateTime.now());
        log.setRole(recoverRole());
        log.setIp(extrairIp(request));
        log.setUserAgent(userAgent);
        log.setNavegador(UserAgentParser.extrairNavegador(userAgent));
        log.setSistemaOperacional(UserAgentParser.extrairSistemaOperacional(userAgent));
        log.setTipoEvento(tipoEvento);
        log.setDescricao(descrever(tipoEvento, status));

        logAcessoRepository.save(log);
    }

    private String recoverUsuarioEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            return usuario.getEmail();
        }
        return null;
    }

    private String recoverRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            return usuario.getRole();
        }
        return null;
    }

    // X-Forwarded-For pode conter uma cadeia de proxies ("cliente, proxy1, proxy2") — o
    // primeiro endereço é sempre o do cliente original.
    private String extrairIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private TipoEventoLog classificarEvento(String metodo, String rota, int status) {
        if (status == 401 || status == 403) return TipoEventoLog.ACESSO_NAO_AUTORIZADO;
        if (status >= 500) return TipoEventoLog.ERRO;

        if (rota.startsWith("/admin")) return TipoEventoLog.ACESSO_PAINEL_ADMIN;
        if (rota.equals("/auth/login")) return TipoEventoLog.LOGIN;
        if (rota.equals("/auth/logout")) return TipoEventoLog.LOGOUT;
        if (rota.equals("/auth/register")) return TipoEventoLog.CADASTRO;

        if (rota.equals("/estacionamentos") && "POST".equals(metodo)) return TipoEventoLog.CRIACAO_ESTACIONAMENTO;
        if (rota.matches("/estacionamentos/\\d+") && "PUT".equals(metodo)) return TipoEventoLog.EDICAO_ESTACIONAMENTO;

        if (rota.equals("/vagas") && "POST".equals(metodo)) return TipoEventoLog.CRIACAO_VAGA;
        if (rota.matches("/vagas/\\d+") && "PUT".equals(metodo)) return TipoEventoLog.EDICAO_VAGA;
        if (rota.matches("/vagas/\\d+") && "DELETE".equals(metodo)) return TipoEventoLog.EXCLUSAO_VAGA;

        if (rota.equals("/estadias") && "POST".equals(metodo)) return TipoEventoLog.CHECKIN;
        if (rota.equals("/estadias/checkin")) return TipoEventoLog.CHECKIN;
        if (rota.equals("/estadias/reservar")) return TipoEventoLog.RESERVA;
        if (rota.matches("/estadias/\\d+/cancelar")) return TipoEventoLog.CANCELAMENTO;
        if (rota.matches("/estadias/\\d+/finalizar")) return TipoEventoLog.CHECKOUT_PAGAMENTO;

        if ("DELETE".equals(metodo)) return TipoEventoLog.EXCLUSAO;

        return TipoEventoLog.OUTRO;
    }

    private String descrever(TipoEventoLog tipoEvento, int status) {
        boolean sucesso = status >= 200 && status < 400;
        return switch (tipoEvento) {
            case LOGIN -> sucesso ? "Login realizado com sucesso" : "Tentativa de login malsucedida";
            case LOGOUT -> "Logout realizado";
            case CADASTRO -> sucesso ? "Novo usuário cadastrado" : "Falha ao cadastrar usuário";
            case CRIACAO_ESTACIONAMENTO -> sucesso ? "Estacionamento cadastrado" : "Falha ao cadastrar estacionamento";
            case EDICAO_ESTACIONAMENTO -> sucesso ? "Estacionamento atualizado" : "Falha ao atualizar estacionamento";
            case CRIACAO_VAGA -> sucesso ? "Vaga criada" : "Falha ao criar vaga";
            case EDICAO_VAGA -> sucesso ? "Vaga atualizada" : "Falha ao atualizar vaga";
            case EXCLUSAO_VAGA -> sucesso ? "Vaga excluída" : "Falha ao excluir vaga";
            case CHECKIN -> sucesso ? "Check-in registrado" : "Falha ao registrar check-in";
            case RESERVA -> sucesso ? "Vaga reservada" : "Falha ao reservar vaga";
            case CANCELAMENTO -> sucesso ? "Reserva cancelada" : "Falha ao cancelar reserva";
            case CHECKOUT_PAGAMENTO -> sucesso ? "Estadia finalizada e cobrança calculada" : "Falha ao finalizar estadia";
            case EXCLUSAO -> sucesso ? "Registro excluído" : "Falha ao excluir registro";
            case ACESSO_PAINEL_ADMIN -> "Acesso ao painel administrativo";
            case ACESSO_NAO_AUTORIZADO -> "Tentativa de acesso sem permissão suficiente";
            case ERRO -> "Erro interno no servidor";
            default -> sucesso ? "Requisição concluída" : "Requisição falhou";
        };
    }
}
