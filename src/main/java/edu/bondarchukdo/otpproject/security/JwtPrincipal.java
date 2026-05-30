package edu.bondarchukdo.otpproject.security;

import edu.bondarchukdo.otpproject.domain.Role;

public record JwtPrincipal(long userId, String login, Role role) {
}
