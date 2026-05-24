package com.antss_prescription.security;

import com.antss_prescription.entity.LoginCredential;
import com.antss_prescription.entity.User;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.repository.LoginCredentialRepository;
import com.antss_prescription.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginCredentialRepository credentialRepository;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        LoginCredential credential = credentialRepository.findByUser(user).orElseThrow(
                () -> new BusinessException("no user credentials found")
        );

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                credential.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}
