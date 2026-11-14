package com.yarnshipment.evidence.domain.model;

import java.io.InputStream;

/**
 * İndirilecek dosya. Akışı alan kapatır.
 *
 * @param mimeType dosya türü
 * @param sizeBytes boyut
 * @param content dosya baytları
 */
public record FileContent(String mimeType, long sizeBytes, InputStream content) {}
