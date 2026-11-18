package com.yarnshipment.assistant.adapter.out.anthropic;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Yardım asistanı ayarları. Anahtar yalnızca ortam değişkeniyle ({@code ANTHROPIC_API_KEY}) verilir; boşsa asistan
 * kapalıdır ve ekranda görünmez.
 *
 * @param apiKey Anthropic API anahtarı (gizli; loglanmaz)
 * @param model kullanılan model
 * @param maxOutputTokens bir cevabın en çok token sayısı
 * @param timeout model servisinden cevap bekleme süresi
 * @param baseUrl model servisi adresi; boşsa varsayılan adres (yalnızca testlerde değiştirilir)
 */
@ConfigurationProperties("yarnshipment.help")
record HelpProperties(
        String apiKey,
        String model,
        long maxOutputTokens,
        Duration timeout,
        @DefaultValue("") String baseUrl) {

    boolean hasApiKey() {
        return !apiKey.isBlank();
    }

    /** SEC-13: anahtar hiçbir yerde yazdırılmaz. */
    @Override
    public String toString() {
        return "HelpProperties[model=" + model + ", maxOutputTokens=" + maxOutputTokens + ", apiKey=***]";
    }
}
