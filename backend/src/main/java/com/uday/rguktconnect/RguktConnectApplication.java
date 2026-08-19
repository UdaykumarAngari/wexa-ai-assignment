package com.uday.rguktconnect;

import com.uday.rguktconnect.entity.User;
import com.uday.rguktconnect.entity.UserDetails;
import com.uday.rguktconnect.repository.user.UserRepository;
import com.uday.rguktconnect.repository.user.UserDetailsRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.uday.rguktconnect.seeder.DatabaseSeeder;

@SpringBootApplication
@EnableScheduling
public class RguktConnectApplication {
	public static void main(String[] args) {
		SpringApplication.run(RguktConnectApplication.class, args);
	}

	@Bean
	public CommandLineRunner initTestData(DatabaseSeeder databaseSeeder) {
		return args -> {
			databaseSeeder.seed();
		};
	}
}
