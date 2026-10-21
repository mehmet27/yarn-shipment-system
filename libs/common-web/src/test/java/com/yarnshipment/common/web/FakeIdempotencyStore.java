package com.yarnshipment.common.web;

import com.yarnshipment.common.web.idempotency.Claim;
import com.yarnshipment.common.web.idempotency.IdempotencyStore;
import com.yarnshipment.common.web.idempotency.StoredResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Bellekte çalışan kayıt defteri; filtre davranışını veritabanı olmadan sınar. */
final class FakeIdempotencyStore implements IdempotencyStore {

    private record Entry(String fingerprint, @Nullable StoredResponse response) {}

    private final Map<String, Entry> entries = new HashMap<>();
    private boolean holdOpen;

    @Override
    public Claim claim(String actor, String key, String fingerprint) {
        Entry entry = entries.get(actor + '/' + key);
        if (entry == null) {
            entries.put(actor + '/' + key, new Entry(fingerprint, null));
            return new Claim.Acquired();
        }
        if (!entry.fingerprint().equals(fingerprint)) {
            return new Claim.Mismatch();
        }
        StoredResponse response = entry.response();
        return response == null ? new Claim.InProgress() : new Claim.Completed(response);
    }

    @Override
    public void complete(String actor, String key, StoredResponse response) {
        if (holdOpen) {
            return;
        }
        Entry entry = Objects.requireNonNull(entries.get(actor + '/' + key));
        entries.put(actor + '/' + key, new Entry(entry.fingerprint(), response));
    }

    @Override
    public void release(String actor, String key) {
        if (holdOpen) {
            return;
        }
        entries.remove(actor + '/' + key);
    }

    /** Sonraki istek bitmiş sayılmaz; anahtar "devam ediyor" durumunda kalır. */
    void holdOpen() {
        holdOpen = true;
    }

    int size() {
        return entries.size();
    }
}
