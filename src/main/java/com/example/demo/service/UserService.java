package com.example.demo.service;

import com.example.demo.dto.UserDto;
import java.util.List;
import java.util.Optional;

public interface UserService {
    List<UserDto> getAllUsers();
    Optional<UserDto> getUserById(Long id);
    UserDto getUserByUsername(String username);
    UserDto createUser(UserDto userDto);
    UserDto updateUser(Long id, UserDto userDto);
    void deleteUser(Long id);
    boolean userExists(String username);
}