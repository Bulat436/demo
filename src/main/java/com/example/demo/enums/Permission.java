package com.example.demo.enums;

import org.springframework.security.core.GrantedAuthority;

public enum Permission implements GrantedAuthority {
    ALERTS_READ, ALERTS_WRITE, ALERTS_DELETE;

    @Override
    public String getAuthority() {
        return name();
    }
}