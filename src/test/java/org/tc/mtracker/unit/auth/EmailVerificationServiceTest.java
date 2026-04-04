package org.tc.mtracker.unit.auth;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tc.mtracker.auth.dto.UpdateEmailRequestDto;
import org.tc.mtracker.auth.mail.AuthEmailService;
import org.tc.mtracker.auth.model.RefreshToken;
import org.tc.mtracker.auth.service.EmailVerificationService;
import org.tc.mtracker.auth.service.RefreshTokenService;
import org.tc.mtracker.security.JwtResponseDTO;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.support.factory.EntityTestFactory;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserRepository;
import org.tc.mtracker.utils.exceptions.EmailVerificationException;
import org.tc.mtracker.utils.exceptions.UserAlreadyActivatedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthEmailService authEmailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    void shouldActivateUserAndReturnTokensForValidVerificationToken() {
        User user = EntityTestFactory.user(1L, "user@example.com", false);
        RefreshToken refreshToken = EntityTestFactory.refreshToken("refresh-token", user, LocalDateTime.now().plusDays(1));

        when(jwtService.extractClaim(eq("verification-token"), any())).thenReturn("email_verification");
        when(jwtService.extractUsername("verification-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        JwtResponseDTO result = emailVerificationService.verifyToken("verification-token");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(user.isActivated()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectAlreadyActivatedUserDuringVerification() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);

        when(jwtService.extractClaim(eq("verification-token"), any())).thenReturn("email_verification");
        when(jwtService.extractUsername("verification-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> emailVerificationService.verifyToken("verification-token"))
                .isInstanceOf(UserAlreadyActivatedException.class);
    }

    @Test
    void shouldStorePendingEmailAndSendVerificationMail() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        UpdateEmailRequestDto dto = new UpdateEmailRequestDto("new@example.com");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(jwtService.generateToken(anyMap(), any())).thenReturn("email-update-token");

        emailVerificationService.updateEmail(dto, user.getEmail());

        assertThat(user.getPendingEmail()).isEqualTo(dto.email());
        assertThat(user.getVerificationToken()).isEqualTo("email-update-token");
        verify(authEmailService).sendVerificationEmail(dto.email(), "email-update-token");
        verify(userRepository).save(user);
    }

    @Test
    void shouldVerifyEmailUpdateWhenTokenMatchesCurrentUserState() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        user.setPendingEmail("new@example.com");
        user.setVerificationToken("email-update-token");

        when(jwtService.extractClaim(eq("email-update-token"), any())).thenReturn("email_update_verification");
        when(jwtService.extractUsername("email-update-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        emailVerificationService.verifyEmailUpdate("email-update-token");

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPendingEmail()).isNull();
        assertThat(user.getVerificationToken()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectEmailUpdateWhenStoredTokenDoesNotMatch() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        user.setPendingEmail("new@example.com");
        user.setVerificationToken("different-token");

        when(jwtService.extractClaim(eq("email-update-token"), any())).thenReturn("email_update_verification");
        when(jwtService.extractUsername("email-update-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> emailVerificationService.verifyEmailUpdate("email-update-token"))
                .isInstanceOf(EmailVerificationException.class);
    }

    @Test
    void shouldRejectVerificationWhenTokenPurposeIsWrong() {
        when(jwtService.extractClaim(eq("wrong-token"), any())).thenReturn("password_reset");

        assertThatThrownBy(() -> emailVerificationService.verifyToken("wrong-token"))
                .isInstanceOf(JwtException.class);
    }
}
