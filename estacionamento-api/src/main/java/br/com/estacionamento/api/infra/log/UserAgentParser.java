package br.com.estacionamento.api.infra.log;



// Extração heurística e leve de navegador/sistema operacional a partir do header
// User-Agent — sem depender de nenhuma biblioteca externa de parsing.
public final class UserAgentParser {

    private UserAgentParser() {}

    public static String extrairNavegador(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return null;
        // Ordem importa: Edge/Opera incluem "Chrome" no próprio UA
        if (userAgent.contains("Edg/")) return "Edge";
        if (userAgent.contains("OPR/") || userAgent.contains("Opera")) return "Opera";
        if (userAgent.contains("Chrome/") && !userAgent.contains("Chromium")) return "Chrome";
        if (userAgent.contains("Firefox/")) return "Firefox";
        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome")) return "Safari";
        return "Outro";
    }

    public static String extrairSistemaOperacional(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return null;
        // Ordem importa: iOS/Android antes das checagens genéricas
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iOS")) return "iOS";
        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac OS") || userAgent.contains("Macintosh")) return "macOS";
        if (userAgent.contains("Linux")) return "Linux";
        return "Outro";
    }
}
