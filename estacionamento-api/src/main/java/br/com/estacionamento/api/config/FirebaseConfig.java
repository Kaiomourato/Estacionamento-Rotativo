package br.com.estacionamento.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

// Inicializa o Firebase Admin SDK a partir da variável de ambiente FIREBASE_CREDENTIALS_JSON
// (conteúdo integral do JSON da conta de serviço, gerado em Firebase Console > Configurações
// do projeto > Contas de serviço > Gerar nova chave privada). Sem essa variável (ex.: rodando
// local sem configurar), o push fica desativado e a aplicação sobe normalmente — só os avisos
// de log indicam que notificações push não serão enviadas.
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${FIREBASE_CREDENTIALS_JSON:}")
    private String credenciaisJson;

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (credenciaisJson == null || credenciaisJson.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS_JSON não definido — notificações push (FCM) ficarão desativadas.");
            return null;
        }
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(credenciaisJson.getBytes(StandardCharsets.UTF_8)));
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        } catch (Exception e) {
            log.error("Falha ao inicializar o Firebase Admin SDK — push ficará desativado.", e);
            return null;
        }
    }
}
