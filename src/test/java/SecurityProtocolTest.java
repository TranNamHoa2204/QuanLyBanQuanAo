import static org.junit.Assert.*;

import java.io.*;
import java.time.*;
import org.junit.Test;
import common.protocol.FrameIO;
import server.security.Pbkdf2PasswordHasher;
import server.service.BusinessException;
import server.session.SessionManager;

public class SecurityProtocolTest {
    @Test public void passwordHashUsesRandomSaltAndRejectsWrongOrMalformed() {
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        String first = hasher.hash("CorrectPass123!".toCharArray());
        String second = hasher.hash("CorrectPass123!".toCharArray());
        assertNotEquals(first, second);
        assertTrue(hasher.verify("CorrectPass123!".toCharArray(), first));
        assertFalse(hasher.verify("wrong".toCharArray(), first));
        assertFalse(hasher.verify("wrong".toCharArray(), "plain text"));
        assertFalse(hasher.verify("wrong".toCharArray(), "pbkdf2-sha256$2147483647$AA==$AA=="));
    }
    @Test public void expiredAndRevokedSessionsAreRejected() {
        MutableClock clock = new MutableClock();
        SessionManager sessions = new SessionManager(clock, Duration.ofMinutes(30));
        String token = sessions.create("sales", "hash");
        assertEquals("sales", sessions.require(token).getUsername());
        clock.now = clock.now.plus(Duration.ofMinutes(30));
        try { sessions.require(token); fail("Expired"); } catch (BusinessException expected) { }
        String next = sessions.create("sales", "hash");
        assertNotEquals(token, next);
        sessions.revokeUser("sales");
        try { sessions.require(next); fail("Revoked"); } catch (BusinessException expected) { }
    }
    @Test public void frameRoundTripAndTruncation() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        FrameIO.write(new DataOutputStream(buffer), "Tiếng Việt".getBytes("UTF-8"));
        assertEquals("Tiếng Việt", new String(FrameIO.read(new DataInputStream(
                new ByteArrayInputStream(buffer.toByteArray()))), "UTF-8"));
        assertNull(FrameIO.read(new DataInputStream(new ByteArrayInputStream(new byte[0]))));
        try {
            FrameIO.read(new DataInputStream(new ByteArrayInputStream(new byte[] {0, 0})));
            fail("Truncated");
        } catch (EOFException expected) { }
    }
    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-10T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
