package org.tc.mtracker.unit.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.tc.mtracker.auth.dto.LoginRequestDto;
import org.tc.mtracker.auth.model.RefreshToken;
import org.tc.mtracker.auth.service.LoginService;
import org.tc.mtracker.auth.service.RefreshTokenService;
import org.tc.mtracker.security.JwtResponseDTO;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.support.factory.EntityTestFactory;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserRepository;
import org.tc.mtracker.utils.exceptions.UserNotActivatedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private LoginService loginService;

    @Test
    void shouldReturnAccessAndRefreshTokensForActivatedUser() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        LoginRequestDto dto = new LoginRequestDto(user.getEmail(), "StrongPass!1");
        RefreshToken refreshToken = EntityTestFactory.refreshToken("refresh-token", user, LocalDateTime.now().plusDays(1));

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        JwtResponseDTO result = loginService.login(dto);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void shouldRejectInactiveUser() {
        User user = EntityTestFactory.user(1L, "inactive@example.com", false);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> loginService.login(new LoginRequestDto(user.getEmail(), "StrongPass!1")))
                .isInstanceOf(UserNotActivatedException.class);

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void shouldRejectWrongPassword() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(new LoginRequestDto(user.getEmail(), "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    void shouldRejectUnknownUser() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(new LoginRequestDto("missing@example.com", "StrongPass!1")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
