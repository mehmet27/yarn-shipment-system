package com.yarnshipment.evidence.domain.model;

/**
 * Nesne deposundan okunan gerçek dosya özellikleri.
 *
 * @param sizeBytes gerçek boyut
 * @param sha256 sunucunun hesapladığı hash (küçük harf, 64 karakter)
 * @param mimeType depoda kayıtlı dosya türü
 */
public record StoredObject(long sizeBytes, String sha256, String mimeType) {}
