package com.kohang.fsi251notifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@ComponentScan("com.kohang")
@EnableMongoRepositories
public class Application {
	
	public static void main(String[] args) {
		
		SpringApplication.run(Application.class, args);	
				
	}

}
