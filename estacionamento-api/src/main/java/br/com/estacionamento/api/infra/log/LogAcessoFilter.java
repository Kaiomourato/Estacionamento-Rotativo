package br.com.estacionamento.api.infra.log;

import br.com.estacionamento.api.model.LogAcesso;
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

@Component
public class LogAcessoFilter extends OncePerRequestFilter {

    @Autowired
    private LogAcessoRepository logAcessoRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        String email = recoverUsuarioEmail();
        LogAcesso log = new LogAcesso(email, request.getMethod(), request.getRequestURI(), response.getStatus(), LocalDateTime.now());
        logAcessoRepository.save(log);
    }

    private String recoverUsuarioEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            return usuario.getEmail();
        }
        return null;
    }
}
