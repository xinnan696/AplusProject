package com.ucd.urbanflow.config;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetails implementation representing the authenticated user.
 * This object is created from the JWT claims and stored in the SecurityContext.
 */
@Data
public class AuthenticatedUser implements UserDetails {

    private Long id;
    private String accountNumber;
    private String userName;
    private String role;

    /**
     * Returns the authorities granted to the user.
     * if role-based authorization is not complex.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // return List.of(new SimpleGrantedAuthority(role));
        return Collections.emptyList();
    }

    /**
     * For a JWT-based stateless authentication, the password is not stored in the principal.
     */
    @Override
    public String getPassword() {
        return null;
    }

    /**
     * Returns the username used to authenticate the user.
     */
    @Override
    public String getUsername() {
        return this.accountNumber;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getUserName() {
        return userName;
    }
}