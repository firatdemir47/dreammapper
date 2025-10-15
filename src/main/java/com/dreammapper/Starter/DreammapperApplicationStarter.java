package com.dreammapper.Starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {"com.dreammapper"})
public class DreammapperApplicationStarter {

	public static void main(String[] args) {
		SpringApplication.run(DreammapperApplicationStarter.class, args);
	}

}
