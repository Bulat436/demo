package com.example.demo.service;

import com.example.demo.dto.UserDto;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserDto> getAllUsers() {
        log.debug("Получение всех пользователей");
        
        List<UserDto> users = userRepository.findAll().stream()
                .map(UserMapper::userToUserDto)
                .toList();
        
        log.info("Получено {} пользователей", users.size());
        return users;
    }

    @Override
    public Optional<UserDto> getUserById(Long id) {
        log.debug("Получение пользователя по ID: {}", id);
        
        Optional<UserDto> user = userRepository.findById(id)
                .map(UserMapper::userToUserDto);
        
        if (user.isPresent()) {
            log.debug("Пользователь найден: id={}, username={}", id, user.get().username());
        } else {
            log.debug("Пользователь не найден: id={}", id);
        }
        
        return user;
    }

    @Override
    public UserDto getUserByUsername(String username) {
        log.debug("Получение пользователя по имени: {}", username);
        
        UserDto user = userRepository.findByUsername(username)
                .map(UserMapper::userToUserDto)
                .orElse(null);
        
        if (user != null) {
            log.debug("Пользователь найден: username={}, role={}", username, user.role());
        } else {
            log.debug("Пользователь не найден: username={}", username);
        }
        
        return user;
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        log.info("Создание нового пользователя: {}", userDto.username());
        try {
            String roleName = userDto.role().replace("ROLE_", "");
            log.debug("Поиск роли: {}", roleName);
            
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> {
                        log.error("Роль не найдена: {}", roleName);
                        return new RuntimeException("Роль не найдена: " + roleName);
                    });
            
            User user = new User();
            user.setUsername(userDto.username());
            user.setPassword(passwordEncoder.encode(userDto.password()));
            user.setRole(role);
            
            User savedUser = userRepository.save(user);
            log.info("Пользователь успешно создан: id={}, username={}, role={}", 
                    savedUser.getId(), savedUser.getUsername(), savedUser.getRole().getAuthority());
            
            return UserMapper.userToUserDto(savedUser);
        } catch (Exception e) {
            log.error("Ошибка создания пользователя: {}, ошибка: {}", userDto.username(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public UserDto updateUser(Long id, UserDto userDto) {
        log.info("Обновление пользователя: id={}, новый username={}", id, userDto.username());
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error("Пользователь не найден для обновления: id={}", id);
                        return new RuntimeException("Пользователь не найден");
                    });
            
            String roleName = userDto.role().replace("ROLE_", "");
            log.debug("Обновление роли на: {}", roleName);
            
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> {
                        log.error("Роль не найдена: {}", roleName);
                        return new RuntimeException("Роль не найдена: " + roleName);
                    });
            
            String oldUsername = user.getUsername();
            user.setUsername(userDto.username());
            
            if (userDto.password() != null && !userDto.password().isEmpty()) {
                log.debug("Обновление пароля для пользователя: {}", userDto.username());
                user.setPassword(passwordEncoder.encode(userDto.password()));
            }
            
            user.setRole(role);
            
            User updatedUser = userRepository.save(user);
            log.info("Пользователь успешно обновлен: id={}, старый username={}, новый username={}, новая роль={}", 
                    id, oldUsername, updatedUser.getUsername(), updatedUser.getRole().getAuthority());
            
            return UserMapper.userToUserDto(updatedUser);
        } catch (Exception e) {
            log.error("Ошибка обновления пользователя: id={}, ошибка: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
        log.info("Удаление пользователя: id={}", id);
        
        try {
            if (!userRepository.existsById(id)) {
                log.warn("Пользователь не найден для удаления: id={}", id);
                return;
            }
            
            log.info("Пользователь успешно удален: id={}", id);
        } catch (Exception e) {
            log.error("Ошибка удаления пользователя: id={}, ошибка: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean userExists(String username) {
        log.debug("Проверка существования пользователя: {}", username);
        
        boolean exists = userRepository.existsByUsername(username);
        log.debug("Проверка существования пользователя - username={}, результат={}", username, exists);
        
        return exists;
    }
}