package com.yarnshipment.evidence.application.port.in;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * İstemcinin dosyayı doğrudan nesne deposuna göndermesi için kısa ömürlü imzalı adres.
 *
 * @param evidenceId kanıt kimliği; onay bu kimlikle yapılır
 * @param uploadUrl imzalı yükleme adresi (PUT)
 * @param headers yüklemede aynen gönderilmesi gereken başlıklar
 * @param expiresAt adresin sona erdiği zaman
 */
public record UploadTicket(UUID evidenceId, String uploadUrl, Map<String, String> headers, Instant expiresAt) {

    public UploadTicket {
        headers = Map.copyOf(headers);
    }
}
