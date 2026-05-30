package edu.bondarchukdo.otpproject.service;

import edu.bondarchukdo.otpproject.config.JwtProperties;
import edu.bondarchukdo.otpproject.dao.UserDao;
import edu.bondarchukdo.otpproject.domain.Role;
import edu.bondarchukdo.otpproject.domain.UserRecord;
import edu.bondarchukdo.otpproject.security.JwtService;
import edu.bondarchukdo.otpproject.service.model.AuthToken;
import edu.bondarchukdo.otpproject.service.model.LoginCredentials;
import edu.bondarchukdo.otpproject.service.model.RegisterCommand;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(
            UserDao userDao, PasswordEncoder passwordEncoder, JwtService jwtService, JwtProperties jwtProperties) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public void register(RegisterCommand command) {
        if (userDao.existsByLogin(command.login())) {
            throw new IllegalArgumentException("Login already taken");
        }
        if (command.role() == Role.ADMIN && userDao.existsAdmin()) {
            throw new IllegalArgumentException("Administrator already exists");
        }
        String hash = passwordEncoder.encode(command.password());
        userDao.insert(
                command.login(),
                hash,
                command.role(),
                command.email(),
                command.phone(),
                command.telegramChatId());
    }

    public AuthToken login(LoginCredentials credentials) {
        UserRecord user = userDao
                .findByLogin(credentials.login())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(credentials.password(), user.passwordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String token = jwtService.createToken(user.id(), user.login(), user.role());
        return new AuthToken(token, jwtProperties.expirationMs() / 1000);
    }
}
