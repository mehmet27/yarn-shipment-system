package com.yarnshipment.evidence.adapter.out.storage;

import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import com.yarnshipment.evidence.domain.model.StoredObject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * {@link ObjectStoragePort}'un AWS SDK v2 ile yazılmış S3 uyumlu (MinIO) uygulaması. Yükleme adresi tür ve boyutu
 * imzaya bağlar; asıl hash'i sunucu nesneyi okuyarak hesaplar (SEC-09).
 */
@Component
final class S3ObjectStorageAdapter implements ObjectStoragePort {

    private static final String HOST_HEADER = "host";

    private final StorageProperties properties;
    private final Clock clock;
    private final S3Client client;
    private final S3Presigner presigner;

    S3ObjectStorageAdapter(StorageProperties properties, Clock clock, S3Client client, S3Presigner presigner) {
        this.properties = properties;
        this.clock = clock;
        this.client = client;
        this.presigner = presigner;
    }

    @Override
    public PresignedUpload presignUpload(UploadTarget target) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(target.storageKey())
                .contentType(target.mimeType())
                .contentLength(target.sizeBytes())
                .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(properties.uploadUrlTtl())
                .putObjectRequest(put)
                .build());
        Map<String, String> headers = new LinkedHashMap<>();
        presigned.signedHeaders().forEach((name, values) -> {
            if (!HOST_HEADER.equalsIgnoreCase(name)) {
                headers.put(name, String.join(",", values));
            }
        });
        return new PresignedUpload(
                presigned.url().toString(), headers, clock.instant().plus(properties.uploadUrlTtl()));
    }

    @Override
    public PresignedDownload presignDownload(String storageKey) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .build();
        PresignedGetObjectRequest presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(properties.downloadUrlTtl())
                .getObjectRequest(get)
                .build());
        return new PresignedDownload(presigned.url().toString(), clock.instant().plus(properties.downloadUrlTtl()));
    }

    @Override
    public Optional<StoredObject> inspect(String storageKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .build();
        try (var stream = client.getObject(request, ResponseTransformer.toInputStream())) {
            GetObjectResponse response = stream.response();
            return Optional.of(ObjectDigest.measure(stream, response.contentType()));
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (SdkException exception) {
            throw new IllegalStateException("Object storage read failed", exception);
        }
    }

    @Override
    public Optional<byte[]> download(String storageKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .build();
        try (var stream = client.getObject(request, ResponseTransformer.toInputStream())) {
            return Optional.of(stream.readAllBytes());
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (SdkException exception) {
            throw new IllegalStateException("Object storage read failed", exception);
        }
    }
}
