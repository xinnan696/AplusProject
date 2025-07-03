package com.ucd.urbanflow.domain.pojo;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * POJO representing the 'users' table in the database.
 * Implements UserDetails for seamless integration with Spring Security.
 */
@Data
public class User implements UserDetails {
    private Long id;
    private String username;
    private String email;
    private String password; // Hashed password
    private String role;     // e.g., "ROLE_USER", "ROLE_ADMIN"
    private boolean locked;
    private boolean enabled;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        // Spring Security uses this as the unique identifier for authentication.
        // We use email as the login identifier.
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !this.locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}