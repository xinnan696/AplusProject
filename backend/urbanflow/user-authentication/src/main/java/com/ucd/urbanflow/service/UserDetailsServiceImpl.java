package com.ucd.urbanflow.service;

import com.ucd.urbanflow.config.AuthenticatedUser;
import com.ucd.urbanflow.domain.pojo.User;
import com.ucd.urbanflow.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementation of UserDetailsService, which is used by Spring Security
 * to fetch user details during authentication.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String accountNumber) throws UsernameNotFoundException {
        User user = userMapper.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with account number: " + accountNumber));

        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setId(user.getId());
        authenticatedUser.setAccountNumber(user.getAccountNumber());
        authenticatedUser.setUserName(user.getUserName());
        authenticatedUser.setEmail(user.getEmail());
        authenticatedUser.setRole(user.getRole());
        authenticatedUser.setPassword(user.getPassword());

        return authenticatedUser;
    }
}