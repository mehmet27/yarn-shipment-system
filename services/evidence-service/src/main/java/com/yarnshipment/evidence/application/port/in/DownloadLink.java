package com.yarnshipment.evidence.application.port.in;

import java.time.Instant;

/**
 * Kanıt dosyasını okumak için kısa ömürlü imzalı adres.
 *
 * @param url GET adresi
 * @param expiresAt adresin sona erdiği zaman
 */
public record DownloadLink(String url, Instant expiresAt) {}
