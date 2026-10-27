package com.yarnshipment.evidence.adapter.out.storage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nesne deposu bağlantı ayarları.
 *
 * @param endpoint S3 uyumlu adres (MinIO)
 * @param region bölge
 * @param bucket kova adı
 * @param accessKey erişim anahtarı
 * @param secretKey gizli anahtar
 * @param pathStyle yol biçimli adresleme (MinIO için true)
 * @param uploadUrlTtl imzalı yükleme adresinin geçerlilik süresi
 */
@ConfigurationProperties("yarnshipment.evidence.storage")
record StorageProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        boolean pathStyle,
        Duration uploadUrlTtl) {}
