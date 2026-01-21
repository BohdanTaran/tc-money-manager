package org.tc.mtracker;

import org.springframework.boot.SpringApplication;

public class TestMtrackerApplication {

	public static void main(String[] args) {
		SpringApplication.from(MtrackerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
