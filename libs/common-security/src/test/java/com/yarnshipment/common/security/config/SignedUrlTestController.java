package com.yarnshipment.common.security.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** İmzalı adresle korunan, oturumsuz ve CSRF belirteçsiz çağrılan örnek uç. */
@RestController
@RequestMapping("/test/signed")
class SignedUrlTestController {

    @PutMapping("/upload")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void upload() {
        // Yetki imzalı adreste olduğundan burada denetim yoktur.
    }
}
