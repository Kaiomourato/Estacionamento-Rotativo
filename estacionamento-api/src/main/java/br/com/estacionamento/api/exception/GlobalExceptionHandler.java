package br.com.estacionamento.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, String>> handleCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }

    // @RequestParam/@PathVariable com tipo incompatível (ex.: vagaId=abc) não deve
    // vazar nomes de classes Java/Spring na resposta.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Parâmetro '" + ex.getName() + "' inválido."));
    }

    // Falhas de Bean Validation (@Valid) em DTOs/entidades de requisição.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erro -> erro.getDefaultMessage())
                .orElse("Dados inválidos.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", mensagem));
    }

    // Erros de SQL/JDBC nunca devem chegar ao cliente com a query/stacktrace crus:
    // loga o detalhe no servidor e devolve uma mensagem genérica.
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccess(DataAccessException ex) {
        log.error("Erro de acesso a dados", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Erro interno ao acessar os dados. Tente novamente mais tarde."));
    }

    // NullPointerException é RuntimeException, mas nunca deveria virar um 400 "esperado":
    // é sempre um bug. Precisa de handler específico (mais específico que o de baixo, então
    // o Spring o escolhe primeiro) para logar em ERROR com stacktrace e não vazar uma
    // mensagem tipo "Cannot invoke ... because ... is null" para o cliente.
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, String>> handleNullPointer(NullPointerException ex) {
        log.error("NullPointerException não tratada", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Erro interno inesperado. Tente novamente."));
    }

    // Regras de negócio (ex.: "Esta vaga já está ocupada") — mensagem já é segura para o
    // cliente, mas também loga em WARN para não ficarem invisíveis nos logs do servidor.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        log.warn("Regra de negócio rejeitou a requisição: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    // Rede de segurança final: qualquer exceção não coberta acima (inclusive checked
    // exceptions) cai aqui em vez do Whitelabel Error Page padrão do Spring, mantendo o
    // mesmo formato { "message": ... } que o frontend já sabe interpretar.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Erro não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Erro interno inesperado. Tente novamente mais tarde."));
    }
}
