package com.madara.security.security.authentication;

import com.madara.security.Exception.type.UnauthorizedException;
import com.madara.security.Exception.type.UserAlreadyExistException;
import com.madara.security.security.jwt.JwtService;
import com.madara.security.model.Role;
import com.madara.security.model.User;
import com.madara.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public void registerUser(RegistrationRequest request, Role role) {

        Optional<User> existing = userRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            throw new UserAlreadyExistException("This Management user is already existing");
        }
        var user = User.builder()
                .name(request.getName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .roles(Collections.singleton(role))
                .isAccountEnabled(true)
                .isAccountNonLocked(true)
                .build();

        userRepository.save(user);
    }

    public String loginAndGenerateJwtToken(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid Email or Password");
        } catch (DisabledException e) {
            throw new UnauthorizedException("Account is disabled");
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("This account is not authenticated");
        }


        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        return jwtService.generateToken(userDetails);

    }


}
