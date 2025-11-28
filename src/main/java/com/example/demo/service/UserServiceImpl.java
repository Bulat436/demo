package com.example.demo.service;

import com.example.demo.dto.UserDto;
import com.example.demo.enums.Role;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserMapper::userToUserDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::userToUserDto);
    }
    
    @Override
    public UserDto getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserMapper::userToUserDto)
                .orElse(null);
    }
    
    @Override
    public UserDto createUser(UserDto userDto) {
        User user = new User();
        user.setUsername(userDto.username());
        user.setPassword(passwordEncoder.encode(userDto.password()));
        user.setRole(Role.valueOf(userDto.role().replace("ROLE_", "")));
        
        User savedUser = userRepository.save(user);
        return UserMapper.userToUserDto(savedUser);
    }
    
    @Override
    public UserDto updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setUsername(userDto.username());
        if (userDto.password() != null && !userDto.password().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.password()));
        }
        user.setRole(Role.valueOf(userDto.role().replace("ROLE_", "")));
        
        User updatedUser = userRepository.save(user);
        return UserMapper.userToUserDto(updatedUser);
    }
    
    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    @Override
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }
}