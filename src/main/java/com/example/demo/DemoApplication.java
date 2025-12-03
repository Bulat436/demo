package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;

import java.util.List;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	CommandLineRunner initData(UserRepository userRepository, 
	                          RoleRepository roleRepository,
	                          PasswordEncoder passwordEncoder) {
		return args -> {
			// Проверяем, есть ли уже пользователи
			if (userRepository.count() == 0) {
				System.out.println("=== CREATING TEST DATA ===");
				
				// Создаем роли
				Role adminRole = new Role();
				adminRole.setName("ADMIN");
				
				Role managerRole = new Role();
				managerRole.setName("MANAGER");
				
				Role userRole = new Role();
				userRole.setName("USER");
				
				List<Role> savedRoles = roleRepository.saveAll(List.of(adminRole, managerRole, userRole));
				System.out.println("Roles created: " + savedRoles.size());
				
				// Создаем пользователей
				User admin = new User();
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setRole(adminRole);
				
				User manager = new User();
				manager.setUsername("manager");
				manager.setPassword(passwordEncoder.encode("manager123"));
				manager.setRole(managerRole);
				
				User user = new User();
				user.setUsername("user");
				user.setPassword(passwordEncoder.encode("user123"));
				user.setRole(userRole);
				
				List<User> savedUsers = userRepository.saveAll(List.of(admin, manager, user));
				System.out.println("Users created: " + savedUsers.size());
				
				// Выводим информацию для отладки
				savedUsers.forEach(u -> {
					System.out.println("User: " + u.getUsername() + " | Role: " + u.getRole().getAuthority());
				});
				System.out.println("=== TEST DATA CREATION COMPLETE ===");
			} else {
				System.out.println("=== DATA ALREADY EXISTS ===");
				userRepository.findAll().forEach(u -> {
					System.out.println("Existing user: " + u.getUsername() + " | Role: " + u.getRole().getAuthority());
				});
			}
		};
	}
}