package org.uteq.backend.seguridad.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisBlacklistService {
    private static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redis;

    public void agregar(String jti, long ttlMs) {
        if (jti == null || jti.trim().isEmpty() || ttlMs <= 0) {
            return;
        }
        redis.opsForValue().set(PREFIX + jti, "revoked", Duration.ofMillis(ttlMs));
    }

    public boolean estaRevocado(String jti) {
        if (jti == null || jti.trim().isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }

    public void revocar(String jti, long tiempoRestanteMs) {
        if (jti != null && !jti.trim().isEmpty() && tiempoRestanteMs > 0) {
            redis.opsForValue().set(PREFIX + jti, "revoked",
                    Duration.ofMillis(tiempoRestanteMs));
        }
    }
}
