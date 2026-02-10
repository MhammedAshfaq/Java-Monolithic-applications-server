package com.javainfraexample.spring_monolith_template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringMonolithTemplateApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringMonolithTemplateApplication.class, args);
	}

}
