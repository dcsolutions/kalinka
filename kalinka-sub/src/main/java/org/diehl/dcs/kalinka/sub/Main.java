package org.diehl.dcs.kalinka.sub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(final String[] args) {

		SpringApplication.run(Main.class, args);
	}
}
