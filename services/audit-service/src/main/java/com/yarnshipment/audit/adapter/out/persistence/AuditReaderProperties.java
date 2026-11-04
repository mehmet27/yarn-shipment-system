package com.yarnshipment.audit.adapter.out.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Audit ekranı için ayrı bağlantı (tek veritabanı kullanıcısı; okuma/yazma yetki ayrımı kalktı).
 *
 * @param url JDBC adresi
 * @param username kullanıcı
 * @param password okuma rolünün parolası
 * @param schema audit tablolarının şeması ({@code audit})
 */
@ConfigurationProperties("yarnshipment.audit.reader")
record AuditReaderProperties(String url, String username, String password, String schema) {}
