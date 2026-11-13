package com.yarnshipment.evidence.adapter.out.storage;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dosya deposu ayarları.
 *
 * @param root kanıt dosyalarının yazıldığı klasör (yedeklenecek yer)
 * @param signingKey yükleme ve indirme adreslerini imzalayan gizli anahtar; en az 32 karakter
 * @param uploadUrlTtl imzalı yükleme adresinin geçerlilik süresi
 * @param downloadUrlTtl imzalı indirme adresinin geçerlilik süresi
 */
@ConfigurationProperties("yarnshipment.evidence.storage")
record StorageProperties(Path root, String signingKey, Duration uploadUrlTtl, Duration downloadUrlTtl) {

    static final int MIN_SIGNING_KEY_LENGTH = 32;

    StorageProperties {
        if (signingKey.length() < MIN_SIGNING_KEY_LENGTH) {
            throw new IllegalArgumentException("yarnshipment.evidence.storage.signing-key must be at least "
                    + MIN_SIGNING_KEY_LENGTH
                    + " characters");
        }
    }
}
