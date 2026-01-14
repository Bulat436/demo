package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
public class DemoApplication {
    private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        log.info("Запуск приложения DemoApplication");
        SpringApplication.run(DemoApplication.class, args);
        log.info("Приложение DemoApplication успешно запущено");
    }

    @Bean
    @Transactional
    CommandLineRunner initData(UserRepository userRepository, 
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("=== СОЗДАНИЕ ТЕСТОВЫХ ДАННЫХ ===");
            
            List<String> roleNames = Arrays.asList("ADMIN", "MANAGER", "USER");
            
            for (String roleName : roleNames) {
                Optional<Role> existingRole = roleRepository.findByName(roleName);
                if (existingRole.isEmpty()) {
                    Role newRole = new Role();
                    newRole.setName(roleName);
                    roleRepository.save(newRole);
                    log.info("Created role: " + roleName);
                } else {
                    log.info("Role already exists: " + roleName);
                }
            }
            
            createUserIfNotExists("admin", "ADMIN", userRepository, roleRepository, passwordEncoder);
            createUserIfNotExists("manager", "MANAGER", userRepository, roleRepository, passwordEncoder);
            createUserIfNotExists("user", "USER", userRepository, roleRepository, passwordEncoder);
            
            log.info("=== DATA INITIALIZATION FINISHED ===");
        };
    }
    
    private void createUserIfNotExists(String username, String roleName,
                                      UserRepository userRepository,
                                      RoleRepository roleRepository,
                                      PasswordEncoder passwordEncoder) {
        
        // Проверяем, существует ли пользователь
        if (userRepository.findByUsername(username).isPresent()) {
            log.info("User already exists: " + username);
            return;
        }
        
        // Получаем роль (она должна существовать после инициализации выше)
        Optional<Role> roleOptional = roleRepository.findByName(roleName);
        if (roleOptional.isEmpty()) {
            log.info("ERROR: Role not found: " + roleName);
            return;
        }
        
        // Создаем пользователя
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(username + "123"));
        user.setRole(roleOptional.get());
        
        userRepository.save(user);
        log.info("Created user: " + username + " with role: " + roleName);
    }
}