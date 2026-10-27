package com.yarnshipment.evidence.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.yarnshipment.evidence.application.port.out.ObjectStoragePort.PresignedUpload;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort.UploadTarget;
import com.yarnshipment.evidence.domain.model.StoredObject;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** İmzalı adres ağ olmadan üretilir; nesne okuma sahte S3 istemcisiyle denenir. */
class S3ObjectStorageAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-27T09:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String KEY = "evidence/2026/10/s/e";
    private static final String HELLO_SHA256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
    private static final long SIZE = 2_048L;

    private final StorageProperties properties = new StorageProperties(
            "http://localhost:9000", "us-east-1", "evidence", "access", "secret-secret", true, TTL);
    private final S3Configuration configuration = new S3Configuration();
    private final S3Client client = mock(S3Client.class);
    private final S3Presigner presigner = configuration.s3Presigner(properties);
    private final S3ObjectStorageAdapter adapter =
            new S3ObjectStorageAdapter(properties, Clock.fixed(NOW, ZoneOffset.UTC), client, presigner);

    @Test
    @DisplayName(
            "İmzalı adres yol biçimli, tür ve boyuta bağlı ve ayar süresi kadar geçerlidir; host başlığı çıkarılır")
    void shouldPresignUpload() {
        PresignedUpload upload = adapter.presignUpload(new UploadTarget(KEY, "image/jpeg", SIZE));

        assertThat(upload.url())
                .startsWith("http://localhost:9000/evidence/" + KEY)
                .contains("X-Amz-Signature=");
        assertThat(upload.headers()).containsEntry("content-type", "image/jpeg").doesNotContainKey("host");
        assertThat(upload.expiresAt()).isEqualTo(NOW.plus(TTL));
    }

    @Test
    @DisplayName("Nesne okunur; boyut ve SHA-256 sunucuda hesaplanır, tür depodan gelir")
    void shouldInspectObject() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        GetObjectResponse response =
                GetObjectResponse.builder().contentType("image/png").build();
        whenReading(call ->
                new ResponseInputStream<>(response, AbortableInputStream.create(new ByteArrayInputStream(bytes))));

        assertThat(adapter.inspect(KEY)).contains(new StoredObject(bytes.length, HELLO_SHA256, "image/png"));
    }

    @Test
    @DisplayName("Nesne yoksa boş döner; depo hatası fırlatılır")
    void shouldHandleMissingObjectAndFailures() {
        whenReading(call -> {
            throw NoSuchKeyException.builder().build();
        });
        assertThat(adapter.inspect(KEY)).isEmpty();

        whenReading(call -> {
            throw SdkClientException.create("down");
        });
        assertThatThrownBy(() -> adapter.inspect(KEY)).isInstanceOf(IllegalStateException.class);
    }

    private void whenReading(Answer<ResponseInputStream<GetObjectResponse>> answer) {
        doAnswer(answer)
                .when(client)
                .getObject(
                        any(GetObjectRequest.class),
                        ArgumentMatchers
                                .<ResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>>>any());
    }

    @Test
    @DisplayName("Yapılandırma istemciyi ve imzalayıcıyı üretir")
    void shouldBuildClients() {
        try (S3Client built = configuration.s3Client(properties)) {
            assertThat(built).isNotNull();
        }
    }
}
