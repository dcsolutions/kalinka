package org.diehl.dcs.kalinka.pub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
@SpringBootApplication
@EnableAutoConfiguration
public class Main {

	public static void main(final String[] args) {

		SpringApplication.run(Main.class, args);
	}
}
