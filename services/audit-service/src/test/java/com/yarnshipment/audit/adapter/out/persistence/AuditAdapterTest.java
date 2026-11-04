package com.yarnshipment.audit.adapter.out.persistence;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit adaptörlerini gerçek şemada sınar. Yazan ve okuyan roller ayrı bağlantılardır; bu yüzden testler transaction
 * dışında çalışır ve kayıtları gerçekten commit eder (her test sınıfı kendi veritabanını açar).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AuditJdbcConfiguration.class, AuditAppendAdapter.class, AuditChainAdapter.class, AuditQueryAdapter.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@interface AuditAdapterTest {}
