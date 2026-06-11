package br.com.estacionamento.api.exception;

public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String message) {
        super(message);
    }
}
