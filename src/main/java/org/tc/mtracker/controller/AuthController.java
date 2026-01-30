package org.tc.mtracker.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tc.mtracker.dto.JwtResponseDTO;
import org.tc.mtracker.dto.UserSignUpRequestDTO;
import org.tc.mtracker.dto.UserSignedUpResponseDTO;
import org.tc.mtracker.services.AuthService;
import org.tc.mtracker.service.AuthService;

@RestController
@RequestMapping(value = "/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<UserSignedUpResponseDTO> signUp(@Valid @RequestBody UserSignUpRequestDTO userSignUpRequestDTO) {
        UserSignedUpResponseDTO userSignedUpResponseDTO = authService.signUp(userSignUpRequestDTO);
        return ResponseEntity.ok().body(userSignedUpResponseDTO);
    }

    @GetMapping("/verify")
    public ResponseEntity<JwtResponseDTO> verifyToken(@RequestParam String token) {
        JwtResponseDTO jwt = authService.verifyToken(token);
        return ResponseEntity.ok().body(jwt);
    }
}
