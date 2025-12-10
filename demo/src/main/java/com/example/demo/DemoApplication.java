package com.example.demo;

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

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    @Transactional
    CommandLineRunner initData(UserRepository userRepository, 
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        return args -> {
            System.out.println("=== INITIALIZING APPLICATION DATA ===");
            
            // Сначала убеждаемся, что роли существуют
            List<String> roleNames = Arrays.asList("ADMIN", "MANAGER", "USER");
            
            for (String roleName : roleNames) {
                Optional<Role> existingRole = roleRepository.findByName(roleName);
                if (existingRole.isEmpty()) {
                    Role newRole = new Role();
                    newRole.setName(roleName);
                    roleRepository.save(newRole);
                    System.out.println("Created role: " + roleName);
                } else {
                    System.out.println("Role already exists: " + roleName);
                }
            }
            
            // Теперь создаем пользователей
            createUserIfNotExists("admin", "ADMIN", userRepository, roleRepository, passwordEncoder);
            createUserIfNotExists("manager", "MANAGER", userRepository, roleRepository, passwordEncoder);
            createUserIfNotExists("user", "USER", userRepository, roleRepository, passwordEncoder);
            
            System.out.println("=== DATA INITIALIZATION FINISHED ===");
        };
    }
    
    private void createUserIfNotExists(String username, String roleName,
                                      UserRepository userRepository,
                                      RoleRepository roleRepository,
                                      PasswordEncoder passwordEncoder) {
        
        // Проверяем, существует ли пользователь
        if (userRepository.findByUsername(username).isPresent()) {
            System.out.println("User already exists: " + username);
            return;
        }
        
        // Получаем роль (она должна существовать после инициализации выше)
        Optional<Role> roleOptional = roleRepository.findByName(roleName);
        if (roleOptional.isEmpty()) {
            System.out.println("ERROR: Role not found: " + roleName);
            return;
        }
        
        // Создаем пользователя
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(username + "123"));
        user.setRole(roleOptional.get());
        
        userRepository.save(user);
        System.out.println("Created user: " + username + " with role: " + roleName);
    }
}