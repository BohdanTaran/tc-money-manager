package org.tc.mtracker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.tc.mtracker.dto.UserSignUpRequestDTO;
import org.tc.mtracker.entity.UserEntity;
import org.tc.mtracker.services.AuthService;

import java.net.URI;

@RestController
@RequestMapping(value = "/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<UserEntity> signUp(@RequestBody UserSignUpRequestDTO userSignUpRequestDTO) {
        UserEntity userEntity = authService.signUp(userSignUpRequestDTO);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();
        URI uri = URI.create(baseUrl + "/" + userEntity.getId());
        return ResponseEntity.created(uri).body(userEntity);
    }
}
