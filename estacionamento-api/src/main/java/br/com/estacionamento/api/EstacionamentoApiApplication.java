package br.com.estacionamento.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EstacionamentoApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EstacionamentoApiApplication.class, args);
	}

}
