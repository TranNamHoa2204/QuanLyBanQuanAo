package server.session;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import common.protocol.ErrorCode;
import server.service.BusinessException;

/** Phiên hết hạn tuyệt đối sau 30 phút; không lấy quyền từ client. */
public final class SessionManager {
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final Duration lifetime;

    public SessionManager() {
        this(Clock.systemUTC(), Duration.ofMinutes(30));
    }

    public SessionManager(Clock clock, Duration lifetime) {
        if (lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("Invalid TTL");
        }
        this.clock = clock;
        this.lifetime = lifetime;
    }

    public synchronized String create(String username, String credentialHash) {
        sessions.entrySet().removeIf(e -> !e.getValue().expiresAt.isAfter(clock.instant()));
        if (sessions.size() >= 10000) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "Hệ thống đã đạt giới hạn phiên.");
        }
        byte[] bytes = new byte[32];
        String token;
        do {
            random.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (sessions.containsKey(token));
        sessions.put(token, new Session(username, credentialHash, clock.instant().plus(lifetime)));
        return token;
    }

    public Session require(String token) {
        Session session = token == null ? null : sessions.get(token);
        if (session == null || !session.expiresAt.isAfter(clock.instant())) {
            revoke(token);
            throw new BusinessException(ErrorCode.UNAUTHENTICATED,
                    "Phiên không hợp lệ hoặc đã hết hạn. Hãy đăng nhập lại.");
        }
        return session;
    }

    public void revoke(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public void revokeUser(String username) {
        sessions.entrySet().removeIf(e -> e.getValue().username.equals(username));
    }

    public static final class Session {
        private final String username;
        private final String credentialHash;
        private final Instant expiresAt;

        private Session(String username, String credentialHash, Instant expiresAt) {
            this.username = username;
            this.credentialHash = credentialHash;
            this.expiresAt = expiresAt;
        }

        public String getUsername() {
            return username;
        }

        public boolean matchesCredential(String hash) {
            return credentialHash.equals(hash);
        }
    }
}
