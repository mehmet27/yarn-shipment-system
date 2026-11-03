package com.yarnshipment.audit.adapter.out.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Okuma rolüyle ayrı bağlantı (yazan rolün SELECT yetkisi yoktur).
 *
 * @param url JDBC adresi
 * @param username okuma rolü ({@code audit_reader})
 * @param password okuma rolünün parolası
 */
@ConfigurationProperties("yarnshipment.audit.reader")
record AuditReaderProperties(String url, String username, String password) {}
