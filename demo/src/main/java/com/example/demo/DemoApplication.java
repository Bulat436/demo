package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;

import java.util.List;

@SpringBootApplication
public class DemoApplication {
    private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        log.info("Запуск приложения DemoApplication");
        SpringApplication.run(DemoApplication.class, args);
        log.info("Приложение DemoApplication успешно запущено");
    }

    @Bean
    @Profile("!test")
    CommandLineRunner initData(UserRepository userRepository, 
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        return args -> {
            if (roleRepository.count() == 0) {
                log.info("=== СОЗДАНИЕ ТЕСТОВЫХ ДАННЫХ ===");
                
                Role adminRole = new Role();
                adminRole.setName("ADMIN");
                
                Role managerRole = new Role();
                managerRole.setName("MANAGER");
                
                Role userRole = new Role();
                userRole.setName("USER");
                
                List<Role> savedRoles = roleRepository.saveAll(List.of(adminRole, managerRole, userRole));
                log.info("Создано ролей: {}", savedRoles.size());
                
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
                log.info("Создано пользователей: {}", savedUsers.size());
                
                savedUsers.forEach(u -> {
                    log.info("Пользователь: {} | Роль: {}", u.getUsername(), u.getRole().getAuthority());
                });
                log.info("=== СОЗДАНИЕ ТЕСТОВЫХ ДАННЫХ ЗАВЕРШЕНО ===");
            } else {
                log.info("=== ДАННЫЕ УЖЕ СУЩЕСТВУЮТ ===");
                userRepository.findAll().forEach(u -> {
                    log.info("Существующий пользователь: {} | Роль: {}", u.getUsername(), u.getRole().getAuthority());
                });
            }
        };
    }
}