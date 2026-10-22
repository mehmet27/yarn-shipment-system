package com.yarnshipment.gateway;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/** Gateway oturum testlerinin kullandığı gerçek Redis; testler boyunca bir kez başlatılır. */
final class TestRedis {

    private static final int PORT = 6379;
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("mirror.gcr.io/library/redis:7")).withExposedPorts(PORT);

    static {
        REDIS.start();
    }

    private TestRedis() {}

    static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(PORT));
        registry.add("spring.data.redis.password", () -> "");
    }
}
