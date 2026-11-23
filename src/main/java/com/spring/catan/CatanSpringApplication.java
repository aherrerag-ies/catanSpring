package com.spring.catan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = { "relacions", "taulell", "herencia" })
@EnableJpaRepositories(basePackages = "com.spring.catan.repository")
public class CatanSpringApplication {
	public static void main(String[] args) {
		SpringApplication.run(CatanSpringApplication.class, args);
	}
}
