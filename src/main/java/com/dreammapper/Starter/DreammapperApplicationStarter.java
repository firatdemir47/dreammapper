package com.dreammapper.Starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.dreammapper"})
@ComponentScan(basePackages = "com.dreammapper")
@EnableJpaRepositories(basePackages = "com.dreammapper")
public class DreammapperApplicationStarter {

	public static void main(String[] args) {
		SpringApplication.run(DreammapperApplicationStarter.class, args);
	}

}
