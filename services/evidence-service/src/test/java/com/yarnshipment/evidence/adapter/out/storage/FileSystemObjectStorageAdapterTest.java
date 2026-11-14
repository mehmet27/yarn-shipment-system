package com.yarnshipment.evidence.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.evidence.application.port.out.ObjectStoragePort.PresignedDownload;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort.PresignedUpload;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort.UploadTarget;
import com.yarnshipment.evidence.domain.model.FileContent;
import com.yarnshipment.evidence.domain.model.SignedFileException;
import com.yarnshipment.evidence.domain.model.SignedFileException.Reason;
import com.yarnshipment.evidence.domain.model.StoredObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Dosya deposu: imzalı adresle yükleme ve indirme, sınır denetimleri ve yol güvenliği (geçici klasörde). */
class FileSystemObjectStorageAdapterTest {

    private static final Instant NOW = Instant.parse("2026-11-13T09:00:00Z");
    private static final Duration UPLOAD_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(5);
    private static final String SIGNING_KEY = "test-signing-key-test-signing-key-0123";
    private static final String KEY =
            "evidence/2026/11/0192f0a0-0000-7000-8000-000000000001/0192f0a0-0000-7000-8000-000000000002";
    private static final String JPEG = "image/jpeg";
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final int PATTERN_PERIOD = 251;
    private static final int JPEG_SIZE = 2_048;
    private static final byte[] JPEG_BYTES = jpeg(JPEG_SIZE);
    private static final byte[] PNG_HEADER = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] HEIC_HEADER = {0, 0, 0, 24, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c'};
    private static final byte[] QUICKTIME_HEADER = {0, 0, 0, 24, 'f', 't', 'y', 'p', 'q', 't', ' ', ' '};
    private static final int SMALL_SIZE = 10;
    private static final int REPEATS = 64;
    private static final int TAMPERED_INDEX = 100;

    @TempDir
    private Path root;

    @Test
    @DisplayName("İmzalı adrese yüklenen dosya onay için okunur: boyut, tür ve SHA-256 sunucuda hesaplanır")
    void shouldStoreUploadedFileAndMeasureIt() throws IOException {
        FileSystemObjectStorageAdapter adapter = adapterAt(NOW);
        PresignedUpload upload = adapter.presignUpload(new UploadTarget(KEY, JPEG, JPEG_BYTES.length));

        assertThat(upload.url()).startsWith(FileSystemObjectStorageAdapter.LINK_PREFIX);
        assertThat(upload.headers()).containsEntry("Content-Type", JPEG);
        assertThat(upload.expiresAt()).isEqualTo(NOW.plus(UPLOAD_TTL));

        adapter.receiveUpload(tokenOf(upload.url()), JPEG, JPEG_BYTES.length, stream(JPEG_BYTES));

        StoredObject stored = adapter.inspect(KEY).orElseThrow();
        assertThat(stored.sizeBytes()).isEqualTo(JPEG_BYTES.length);
        assertThat(stored.mimeType()).isEqualTo(JPEG);
        assertThat(stored.sha256()).isEqualTo(sha256Of(JPEG_BYTES));
        assertThat(adapter.download(KEY)).contains(JPEG_BYTES);
        assertThat(root.resolve(KEY)).isRegularFile();
        assertThat(leftovers()).isEmpty();
    }

    @Test
    @DisplayName("İndirme adresi dosyayı, kendi türü ve boyutuyla verir; süresi indirme ayarı kadardır")
    void shouldServeDownloadLink() throws IOException {
        FileSystemObjectStorageAdapter adapter = adapterAt(NOW);
        store(adapter);

        PresignedDownload download = adapter.presignDownload(KEY);
        FileContent content = adapter.openDownload(tokenOf(download.url()));
        try (var stream = content.content()) {
            assertThat(content.mimeType()).isEqualTo(JPEG);
            assertThat(content.sizeBytes()).isEqualTo(JPEG_BYTES.length);
            assertThat(stream.readAllBytes()).isEqualTo(JPEG_BYTES);
        }
        assertThat(download.expiresAt()).isEqualTo(NOW.plus(DOWNLOAD_TTL));
    }

    @Test
    @DisplayName("Boyutu bildirilenden farklı ya da türü uyuşmayan yükleme reddedilir ve dosya bırakmaz")
    void shouldRejectMismatchedUploads() throws IOException {
        FileSystemObjectStorageAdapter adapter = adapterAt(NOW);
        String token = tokenOf(adapter.presignUpload(new UploadTarget(KEY, JPEG, JPEG_BYTES.length))
                .url());

        assertRefused(Reason.SIZE_MISMATCH, () -> adapter.receiveUpload(token, JPEG, 1, stream(JPEG_BYTES)));
        assertRefused(
                Reason.SIZE_MISMATCH,
                () -> adapter.receiveUpload(token, JPEG, -1, stream(jpeg(JPEG_BYTES.length + 1))));
        assertRefused(
                Reason.SIZE_MISMATCH,
                () -> adapter.receiveUpload(token, JPEG, -1, stream(jpeg(JPEG_BYTES.length - 1))));
        assertRefused(
                Reason.TYPE_MISMATCH,
                () -> adapter.receiveUpload(token, "image/png", JPEG_BYTES.length, stream(JPEG_BYTES)));
        assertRefused(
                Reason.TYPE_MISMATCH, () -> adapter.receiveUpload(token, null, JPEG_BYTES.length, stream(JPEG_BYTES)));
        byte[] disguised = "<html>not an image at all</html>".repeat(REPEATS).getBytes(StandardCharsets.UTF_8);
        String disguisedToken = tokenOf(adapter.presignUpload(new UploadTarget(KEY, JPEG, disguised.length))
                .url());
        assertRefused(
                Reason.TYPE_MISMATCH,
                () -> adapter.receiveUpload(disguisedToken, JPEG, disguised.length, stream(disguised)));

        assertThat(adapter.inspect(KEY)).isEmpty();
        assertThat(leftovers()).isEmpty();
    }

    @Test
    @DisplayName("Kurcalanmış, süresi dolmuş ya da yanlış işlem için verilmiş adres reddedilir")
    void shouldRefuseBadLinks() throws IOException {
        FileSystemObjectStorageAdapter adapter = adapterAt(NOW);
        PresignedUpload upload = adapter.presignUpload(new UploadTarget(KEY, JPEG, JPEG_BYTES.length));
        String token = tokenOf(upload.url());

        String tampered = token.substring(0, token.length() - 2) + "AA";
        assertRefused(Reason.INVALID_LINK, () -> adapter.receiveUpload(tampered, JPEG, -1, stream(JPEG_BYTES)));
        assertRefused(Reason.INVALID_LINK, () -> adapter.receiveUpload("sacma", JPEG, -1, stream(JPEG_BYTES)));
        assertRefused(Reason.INVALID_LINK, () -> adapter.openDownload(token));
        FileSystemObjectStorageAdapter other = adapterWithKey("another-signing-key-another-signing-key-12", NOW);
        assertRefused(Reason.INVALID_LINK, () -> other.receiveUpload(token, JPEG, -1, stream(JPEG_BYTES)));

        FileSystemObjectStorageAdapter later = adapterAt(NOW.plus(UPLOAD_TTL).plusSeconds(1));
        assertRefused(Reason.EXPIRED_LINK, () -> later.receiveUpload(token, JPEG, -1, stream(JPEG_BYTES)));

        store(adapter);
        String downloadToken = tokenOf(adapter.presignDownload(KEY).url());
        assertRefused(Reason.INVALID_LINK, () -> adapter.receiveUpload(downloadToken, JPEG, -1, stream(JPEG_BYTES)));
        assertRefused(Reason.EXPIRED_LINK, () -> later.openDownload(downloadToken));
    }

    @Test
    @DisplayName("Dosya yoksa indirme FILE_NOT_FOUND, onay okuması boş döner; var olan dosyanın üzerine yazılmaz")
    void shouldHandleMissingAndExistingFiles() throws IOException {
        FileSystemObjectStorageAdapter adapter = adapterAt(NOW);
        String downloadToken = tokenOf(adapter.presignDownload(KEY).url());
        assertRefused(Reason.FILE_NOT_FOUND, () -> adapter.openDownload(downloadToken));
        assertThat(adapter.inspect(KEY)).isEmpty();
        assertThat(adapter.download(KEY)).isEmpty();

        store(adapter);
        byte[] other = jpeg(JPEG_BYTES.length);
        other[TAMPERED_INDEX] = 1;
        String token = tokenOf(
                adapter.presignUpload(new UploadTarget(KEY, JPEG, other.length)).url());
        adapter.receiveUpload(token, JPEG, other.length, stream(other));
        assertThat(adapter.download(KEY)).contains(JPEG_BYTES);
    }

    @Test
    @DisplayName("Beklenen desene uymayan anahtar (yol geçişi dahil) hiçbir işlemde kabul edilmez")
    void shouldRejectUnexpectedKeys() {
        FileSystemObjectStorageAdapter adapter = adapterAt(NOW);
        List<String> keys = List.of(
                "../../etc/passwd",
                "evidence/2026/11/../../../x",
                "/evidence/2026/11/a/b",
                KEY + "/../other",
                KEY + "x");

        for (String key : keys) {
            assertThatThrownBy(() -> adapter.presignUpload(new UploadTarget(key, JPEG, SMALL_SIZE)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> adapter.presignDownload(key)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> adapter.inspect(key)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> adapter.download(key)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("Dosya türü ilk baytlardan tanınır; tanınmayan tür octet-stream olur")
    void shouldSniffFileTypes() {
        assertThat(FileTypeSniffer.sniff(jpeg(SMALL_SIZE))).isEqualTo(JPEG);
        assertThat(FileTypeSniffer.sniff(PNG_HEADER)).isEqualTo("image/png");
        assertThat(FileTypeSniffer.sniff("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII)))
                .isEqualTo("application/pdf");
        assertThat(FileTypeSniffer.sniff(HEIC_HEADER)).isEqualTo("image/heic");
        assertThat(FileTypeSniffer.sniff(QUICKTIME_HEADER)).isEqualTo(FileTypeSniffer.UNKNOWN);
        assertThat(FileTypeSniffer.sniff(new byte[0])).isEqualTo(FileTypeSniffer.UNKNOWN);
    }

    @Test
    @DisplayName("Kısa imza anahtarı açılışta reddedilir")
    void shouldRejectShortSigningKey() {
        assertThatThrownBy(() -> new StorageProperties(root, "short", UPLOAD_TTL, DOWNLOAD_TTL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void store(FileSystemObjectStorageAdapter adapter) {
        String token = tokenOf(adapter.presignUpload(new UploadTarget(KEY, JPEG, JPEG_BYTES.length))
                .url());
        adapter.receiveUpload(token, JPEG, JPEG_BYTES.length, stream(JPEG_BYTES));
    }

    private FileSystemObjectStorageAdapter adapterAt(Instant now) {
        return adapterWithKey(SIGNING_KEY, now);
    }

    private FileSystemObjectStorageAdapter adapterWithKey(String signingKey, Instant now) {
        StorageProperties properties = new StorageProperties(root, signingKey, UPLOAD_TTL, DOWNLOAD_TTL);
        return new FileSystemObjectStorageAdapter(properties, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static void assertRefused(Reason reason, ThrowingCall call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        SignedFileException.class,
                        exception -> assertThat(exception.reason()).isEqualTo(reason));
    }

    private List<Path> leftovers() throws IOException {
        Path temporary = root.resolve(".tmp");
        if (!Files.exists(temporary)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(temporary)) {
            return files.toList();
        }
    }

    private static String tokenOf(String url) {
        return url.substring(FileSystemObjectStorageAdapter.LINK_PREFIX.length());
    }

    private static ByteArrayInputStream stream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    private static byte[] jpeg(int size) {
        byte[] bytes = new byte[size];
        for (int index = 0; index < size; index++) {
            bytes[index] = index < JPEG_MAGIC.length ? JPEG_MAGIC[index] : (byte) (index % PATTERN_PERIOD);
        }
        return bytes;
    }

    private static String sha256Of(byte[] bytes) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(
                            java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** Fırlatması beklenen çağrı. */
    @FunctionalInterface
    private interface ThrowingCall {
        void run() throws IOException;
    }
}
