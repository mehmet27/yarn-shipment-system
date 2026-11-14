package com.yarnshipment.evidence.adapter.out.storage;

import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import com.yarnshipment.evidence.domain.model.FileContent;
import com.yarnshipment.evidence.domain.model.SignedFileException;
import com.yarnshipment.evidence.domain.model.SignedFileException.Reason;
import com.yarnshipment.evidence.domain.model.StoredObject;
import com.yarnshipment.evidence.domain.model.UploadRules;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * {@link ObjectStoragePort}'un sunucu diskindeki klasörle çalışan uygulaması. Dosya, anahtarın yoluna yazılır
 * ({@code evidence/yıl/ay/sevkiyat/kanıt}); yükleme ve indirme adreslerini uygulama HMAC ile imzalar. Yükleme önce
 * geçici dosyaya yazılır, boyutu ve türü doğrulanınca yerine taşınır; var olan dosyanın üzerine yazılmaz. Asıl hash'i
 * sunucu dosyayı okuyarak hesaplar (SEC-09).
 */
@Component
final class FileSystemObjectStorageAdapter implements ObjectStoragePort {

    /** Adresin yolu; {@code EvidenceFileController} ile aynı olmalıdır. */
    static final String LINK_PREFIX = "/api/v1/evidence/files/";

    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final Pattern KEY_PATTERN =
            Pattern.compile("evidence/\\d{4}/\\d{2}/" + UUID_PATTERN + "/" + UUID_PATTERN);
    private static final String TEMP_DIRECTORY = ".tmp";
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final Set<PosixFilePermission> READ_ONLY = PosixFilePermissions.fromString("r--r-----");

    private final StorageProperties properties;
    private final Clock clock;
    private final FileGrantSigner signer;
    private final Path root;

    FileSystemObjectStorageAdapter(StorageProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signer = new FileGrantSigner(properties.signingKey());
        this.root = properties.root().toAbsolutePath().normalize();
    }

    @Override
    public PresignedUpload presignUpload(UploadTarget target) {
        requireValidKey(target.storageKey());
        var expiresAt = clock.instant().plus(properties.uploadUrlTtl());
        var grant = new FileGrant(
                FileGrant.Operation.UPLOAD, target.storageKey(), target.mimeType(), target.sizeBytes(), expiresAt);
        return new PresignedUpload(
                LINK_PREFIX + signer.sign(grant), Map.of("Content-Type", target.mimeType()), expiresAt);
    }

    @Override
    public PresignedDownload presignDownload(String storageKey) {
        requireValidKey(storageKey);
        var expiresAt = clock.instant().plus(properties.downloadUrlTtl());
        var grant = new FileGrant(FileGrant.Operation.DOWNLOAD, storageKey, "", 0, expiresAt);
        return new PresignedDownload(LINK_PREFIX + signer.sign(grant), expiresAt);
    }

    @Override
    public void receiveUpload(String token, @Nullable String contentType, long contentLength, InputStream body) {
        FileGrant grant = signer.verify(token, clock.instant());
        if (grant.operation() != FileGrant.Operation.UPLOAD) {
            throw new SignedFileException(Reason.INVALID_LINK);
        }
        if (contentLength >= 0 && contentLength != grant.sizeBytes()) {
            throw new SignedFileException(Reason.SIZE_MISMATCH);
        }
        if (!sameMimeType(contentType, grant.mimeType())) {
            throw new SignedFileException(Reason.TYPE_MISMATCH);
        }
        Path target = resolve(grant.storageKey());
        if (Files.exists(target)) {
            return;
        }
        try {
            storeAtomically(body, grant, target);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void storeAtomically(InputStream body, FileGrant grant, Path target) throws IOException {
        Path temporary = temporaryFile();
        try {
            copyExactly(body, temporary, grant.sizeBytes());
            if (!grant.mimeType().equals(FileTypeSniffer.sniff(temporary))) {
                throw new SignedFileException(Reason.TYPE_MISMATCH);
            }
            publish(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public FileContent openDownload(String token) {
        FileGrant grant = signer.verify(token, clock.instant());
        if (grant.operation() != FileGrant.Operation.DOWNLOAD) {
            throw new SignedFileException(Reason.INVALID_LINK);
        }
        Path file = resolve(grant.storageKey());
        try {
            String mimeType = FileTypeSniffer.sniff(file);
            return new FileContent(mimeType, Files.size(file), Files.newInputStream(file));
        } catch (NoSuchFileException exception) {
            throw new SignedFileException(Reason.FILE_NOT_FOUND);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public Optional<StoredObject> inspect(String storageKey) {
        Path file = resolve(storageKey);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (InputStream stream = Files.newInputStream(file)) {
            return Optional.of(ObjectDigest.measure(stream, FileTypeSniffer.sniff(file)));
        } catch (NoSuchFileException exception) {
            return Optional.empty();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public Optional<byte[]> download(String storageKey) {
        try {
            return Optional.of(Files.readAllBytes(resolve(storageKey)));
        } catch (NoSuchFileException exception) {
            return Optional.empty();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** Anahtarı yalnızca beklenen desenle kabul eder ve kök klasörün dışına çıkamayacak yolu verir. */
    private Path resolve(String storageKey) {
        requireValidKey(storageKey);
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Storage key escapes the storage root");
        }
        return path;
    }

    private static void requireValidKey(String storageKey) {
        if (!KEY_PATTERN.matcher(storageKey).matches()) {
            throw new IllegalArgumentException("Unexpected storage key: " + storageKey);
        }
    }

    private static boolean sameMimeType(@Nullable String declared, String expected) {
        if (declared == null) {
            return false;
        }
        int parameters = declared.indexOf(';');
        String bare = (parameters < 0 ? declared : declared.substring(0, parameters)).trim();
        return bare.toLowerCase(Locale.ROOT).equals(expected);
    }

    private Path temporaryFile() throws IOException {
        Path directory = root.resolve(TEMP_DIRECTORY);
        Files.createDirectories(directory);
        return Files.createTempFile(directory, "upload-", ".part");
    }

    /** Tam {@code expectedSize} bayt yazar; fazlası ya da eksiği boyut uyuşmazlığıdır. Üst sınır da zorlanır. */
    private static void copyExactly(InputStream body, Path temporary, long expectedSize) throws IOException {
        if (!UploadRules.isAllowedSize(expectedSize)) {
            throw new SignedFileException(Reason.SIZE_MISMATCH);
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        try (OutputStream out = Files.newOutputStream(temporary)) {
            int read;
            while ((read = body.read(buffer)) != -1) {
                total += read;
                if (total > expectedSize) {
                    throw new SignedFileException(Reason.SIZE_MISMATCH);
                }
                out.write(buffer, 0, read);
            }
        }
        if (total != expectedSize) {
            throw new SignedFileException(Reason.SIZE_MISMATCH);
        }
    }

    /** Dosyayı yerine taşır; zaten varsa (eşzamanlı tekrar gönderim) dokunmaz. Yazılan dosya salt okunur yapılır. */
    private static void publish(Path temporary, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try {
            Files.move(temporary, target);
            markReadOnly(target);
        } catch (FileAlreadyExistsException ignored) {
            // Aynı adrese eşzamanlı ikinci gönderim: ilk yazılan dosya kalır.
            Files.deleteIfExists(temporary);
        }
    }

    private static void markReadOnly(Path target) throws IOException {
        try {
            Files.setPosixFilePermissions(target, READ_ONLY);
        } catch (UnsupportedOperationException ignored) {
            // Salt okunur izni olmayan dosya sistemlerinde yok sayılır; silinmezlik uygulama kuralıdır.
            return;
        }
    }
}
