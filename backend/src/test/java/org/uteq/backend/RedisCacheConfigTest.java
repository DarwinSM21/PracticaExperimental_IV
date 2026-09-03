package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.uteq.backend.config.RedisCacheConfig;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisCacheConfigTest {

    @Test
    @DisplayName("cacheManager inicializa correctamente y resuelve las instancias de cache configuradas")
    void cacheManager_creaYResuelveCaches() throws Exception {
        RedisCacheConfig config = new RedisCacheConfig();

        Field ttlEstudiantes = RedisCacheConfig.class.getDeclaredField("ttlEstudiantesSeconds");
        ttlEstudiantes.setAccessible(true);
        ttlEstudiantes.setLong(config, 120L);

        Field ttlEntrenadores = RedisCacheConfig.class.getDeclaredField("ttlEntrenadoresSeconds");
        ttlEntrenadores.setAccessible(true);
        ttlEntrenadores.setLong(config, 90L);

        Field ttlUsuarios = RedisCacheConfig.class.getDeclaredField("ttlUsuariosSeconds");
        ttlUsuarios.setAccessible(true);
        ttlUsuarios.setLong(config, 60L);

        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(factory.getConnection()).thenReturn(connection);

        RedisCacheManager manager = config.cacheManager(factory);
        manager.afterPropertiesSet();

        assertThat(manager).isNotNull();

        Cache cacheEstudiantes = manager.getCache(RedisCacheConfig.CACHE_ESTUDIANTES);
        Cache cacheEntrenadores = manager.getCache(RedisCacheConfig.CACHE_ENTRENADORES);
        Cache cacheUsuarios = manager.getCache(RedisCacheConfig.CACHE_USUARIOS);

        assertThat(cacheEstudiantes).isNotNull();
        assertThat(cacheEntrenadores).isNotNull();
        assertThat(cacheUsuarios).isNotNull();

        assertThat(cacheEstudiantes.getName()).isEqualTo("estudiantes");
        assertThat(cacheEntrenadores.getName()).isEqualTo("entrenadores");
        assertThat(cacheUsuarios.getName()).isEqualTo("usuarios");
    }
}
