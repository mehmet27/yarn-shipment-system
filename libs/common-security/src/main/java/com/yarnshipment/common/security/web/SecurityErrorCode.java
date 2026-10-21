package com.yarnshipment.common.security.web;

import java.util.Locale;
import org.springframework.http.HttpStatus;

/** Güvenlik zincirinin ürettiği hata kodları; frontend kararını bu koda göre verir (API-08). */
public enum SecurityErrorCode {
    /** Token yok, süresi dolmuş veya kurallara uymuyor. */
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED),
    /** Rolün gereken izni yok. */
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    /** Kayıt aktörün kapsamı dışında (SEC-03). */
    OUT_OF_SCOPE(HttpStatus.FORBIDDEN);

    private final HttpStatus status;

    SecurityErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    /** ERR-07 {@code type} adresinin son parçası, ör. {@code out-of-scope}. */
    public String slug() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
