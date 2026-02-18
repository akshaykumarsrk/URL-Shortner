package com.practice.url_shortner.service;

import com.practice.url_shortner.dto.UserRequest;
import com.practice.url_shortner.model.User;
import com.practice.url_shortner.repository.UserRepository;
import com.practice.url_shortner.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtUtil jwtUtil;

    public String register(UserRequest userRequest) {

        if(userRepository.findByEmail(userRequest.getEmail()).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User();
        user.setEmail(userRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user.setRole("USER");

        userRepository.save(user);

        return "User registered successfully";
    }

    public String login(UserRequest userRequest) {

        // 1️⃣ Find user by email
        User user = userRepository.findByEmail(userRequest.getEmail())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        // 2️⃣ Verify password
        boolean isPasswordCorrect = passwordEncoder.matches(userRequest.getPassword(), user.getPassword());

        if(!isPasswordCorrect){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        // 3️⃣ Generate JWT token
        return jwtUtil.generateToken(user.getEmail());
    }
}
