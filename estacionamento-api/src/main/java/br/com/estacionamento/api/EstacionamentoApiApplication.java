package br.com.estacionamento.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class EstacionamentoApiApplication {

	// O host (Render) roda a JVM em UTC, mas todo o código usa LocalDateTime.now()
	// assumindo horário de Brasília (ex.: TokenService fixa offset "-03:00").
	// Sem isso, toda hora gravada/exibida sai adiantada em 3h. Precisa ser setado
	// antes de qualquer LocalDateTime.now(), por isso é a primeira linha do main.
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
	}

	public static void main(String[] args) {
		SpringApplication.run(EstacionamentoApiApplication.class, args);
	}

}
