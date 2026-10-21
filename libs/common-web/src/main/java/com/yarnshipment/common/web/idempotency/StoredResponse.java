package com.yarnshipment.common.web.idempotency;

import org.jspecify.annotations.Nullable;

/** İlk isteğin başarılı yanıtı; aynı anahtarla gelen tekrarda olduğu gibi döner. */
public final class StoredResponse {

    private final int status;
    private final @Nullable String contentType;
    private final @Nullable String location;
    private final byte[] body;

    public StoredResponse(int status, @Nullable String contentType, @Nullable String location, byte[] body) {
        this.status = status;
        this.contentType = contentType;
        this.location = location;
        this.body = body.clone();
    }

    public int status() {
        return status;
    }

    public @Nullable String contentType() {
        return contentType;
    }

    public @Nullable String location() {
        return location;
    }

    public byte[] body() {
        return body.clone();
    }
}
