package edu.bondarchukdo.otpproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.bondarchukdo.otpproject.config.JwtProperties;
import edu.bondarchukdo.otpproject.dao.UserDao;
import edu.bondarchukdo.otpproject.domain.Role;
import edu.bondarchukdo.otpproject.domain.UserRecord;
import edu.bondarchukdo.otpproject.security.JwtService;
import edu.bondarchukdo.otpproject.service.model.AuthToken;
import edu.bondarchukdo.otpproject.service.model.LoginCredentials;
import edu.bondarchukdo.otpproject.service.model.RegisterCommand;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerInsertsUserWhenLoginIsFree() {
        RegisterCommand command =
                new RegisterCommand("alice", "secret", Role.USER, "a@example.com", null, null);
        when(userDao.existsByLogin("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hash");

        authService.register(command);

        verify(userDao).insert("alice", "hash", Role.USER, "a@example.com", null, null);
    }

    @Test
    void registerRejectsDuplicateLogin() {
        RegisterCommand command = new RegisterCommand("alice", "secret", Role.USER, null, null, null);
        when(userDao.existsByLogin("alice")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(command));
        verify(userDao, never()).insert(any(), any(), any(), any(), any(), any());
    }

    @Test
    void registerRejectsSecondAdmin() {
        RegisterCommand command = new RegisterCommand("admin2", "secret", Role.ADMIN, null, null, null);
        when(userDao.existsByLogin("admin2")).thenReturn(false);
        when(userDao.existsAdmin()).thenReturn(true);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> authService.register(command));
        assertEquals("Administrator already exists", ex.getMessage());
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        UserRecord user = new UserRecord(1L, "alice", "hash", Role.USER, null, null, null);
        when(userDao.findByLogin("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.createToken(1L, "alice", Role.USER)).thenReturn("jwt-token");
        when(jwtProperties.expirationMs()).thenReturn(86_400_000L);

        AuthToken token = authService.login(new LoginCredentials("alice", "secret"));

        assertEquals("jwt-token", token.accessToken());
        assertEquals(86_400L, token.expiresInSeconds());
    }

    @Test
    void loginRejectsUnknownUser() {
        when(userDao.findByLogin("alice")).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(new LoginCredentials("alice", "secret")));
    }

    @Test
    void loginRejectsWrongPassword() {
        UserRecord user = new UserRecord(1L, "alice", "hash", Role.USER, null, null, null);
        when(userDao.findByLogin("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(new LoginCredentials("alice", "wrong")));
    }
}
