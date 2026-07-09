package br.com.estacionamento.api.model;

// Classificação semântica de cada requisição registrada em LogAcesso, usada
// pela tela de Auditoria (filtro por tipo de evento).
public enum TipoEventoLog {
    LOGIN,
    LOGOUT,
    CADASTRO,
    CRIACAO_ESTACIONAMENTO,
    EDICAO_ESTACIONAMENTO,
    CRIACAO_VAGA,
    EDICAO_VAGA,
    EXCLUSAO_VAGA,
    CHECKIN,
    RESERVA,
    CANCELAMENTO,
    CHECKOUT_PAGAMENTO,
    EXCLUSAO,
    ACESSO_PAINEL_ADMIN,
    ACESSO_NAO_AUTORIZADO,
    ERRO,
    OUTRO
}
