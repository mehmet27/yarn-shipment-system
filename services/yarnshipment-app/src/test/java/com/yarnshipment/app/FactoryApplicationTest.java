package com.yarnshipment.app;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;

/**
 * Uygulamanın tamamı gerçek PostgreSQL ile açılır ve HTTP uçları üzerinden sürülür. Bu ek açıklamayı taşıyan sınıflar
 * aynı Spring bağlamını paylaşır; {@link Factory} ve {@link FactoryHttp} bağlamın bileşenleridir.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        properties = {
            "yarnshipment.evidence.storage.signing-key=test-signing-key-test-signing-key-0123",
            "yarnshipment.evidence.storage.root=target/evidence-test",
            "yarnshipment.auth.bootstrap.admin-password=" + FactoryHttp.BOOTSTRAP_PASSWORD
        })
@AutoConfigureMockMvc
@ContextConfiguration(initializers = FactoryApplicationTest.Database.class)
@Import({FactoryHttp.class, Factory.class})
@interface FactoryApplicationTest {

    /** Testcontainers veritabanının adresini bağlama verir ({@code @DynamicPropertySource} sınıfa bağlı kalırdı). */
    final class Database implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource("factory-test-database", PostgresTestDatabase.properties()));
        }
    }
}
