package com.example.demo.mapper;

import com.example.demo.dto.UserDto;
import com.example.demo.dto.UserLoggedDto;
import com.example.demo.model.User;

import java.util.Collections;

public class UserMapper {
    public static UserDto userToUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole().getAuthority(),
                Collections.emptySet()
        );
    }
    
    public static User userDtoToUser(UserDto dto) {
        User user = new User();
        user.setUsername(dto.username());
        return user;
    }
    
    public static UserLoggedDto userToUserLoggedDto(User user) {
        return new UserLoggedDto(
                user.getUsername(),
                user.getRole().getAuthority(),
                Collections.emptySet()
        );
    }
}