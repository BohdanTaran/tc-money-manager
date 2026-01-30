package org.tc.mtracker.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tc.mtracker.dto.JwtResponseDTO;
import org.tc.mtracker.dto.UserSignUpRequestDTO;
import org.tc.mtracker.services.AuthService;

@RestController
@RequestMapping(value = "/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<JwtResponseDTO> signUp(@Valid @RequestBody UserSignUpRequestDTO userSignUpRequestDTO) {
        JwtResponseDTO token = authService.signUp(userSignUpRequestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }
}
