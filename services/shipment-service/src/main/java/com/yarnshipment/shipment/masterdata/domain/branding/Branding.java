package com.yarnshipment.shipment.masterdata.domain.branding;

import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Kurulumun marka ayarı (her firma ayrı kurulumdur, tek kayıt vardır).
 *
 * @param companyName firma adı; boşsa yok sayılır, en çok 80 karakter
 * @param primaryColor ana renk, küçük harfli {@code #rrggbb}
 * @param logoDataUrl logo, base64 {@code data:image/...} adresi; en çok 200 000 karakter
 */
public record Branding(
        @Nullable String companyName,
        String primaryColor,
        @Nullable String logoDataUrl) {

    /** Marka ayarı kaydedilmemişse kullanılan ana renk (arayüzdeki varsayılanla aynıdır). */
    public static final String DEFAULT_COLOR = "#17527a";

    static final int MAX_NAME_LENGTH = 80;
    static final int MAX_LOGO_LENGTH = 200_000;
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Pattern LOGO = Pattern.compile("data:image/(png|jpeg|webp|svg\\+xml);base64,[A-Za-z0-9+/=]+");

    /** Desenler tanımlandıktan sonra gelmeli: kurucu bunları kullanır (sınıf başlatma sırası). */
    public static final Branding DEFAULT = new Branding(null, DEFAULT_COLOR, null);

    public Branding {
        Objects.requireNonNull(primaryColor, "primaryColor");
        if (!COLOR.matcher(primaryColor).matches()) {
            throw new InvalidMasterDataException("primaryColor", "not a #rrggbb color");
        }
        primaryColor = primaryColor.toLowerCase(Locale.ROOT);
        companyName = cleanName(companyName);
        logoDataUrl = cleanLogo(logoDataUrl);
    }

    private static @Nullable String cleanName(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.strip();
        if (text.length() > MAX_NAME_LENGTH) {
            throw new InvalidMasterDataException("companyName", "too long");
        }
        return text;
    }

    private static @Nullable String cleanLogo(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() > MAX_LOGO_LENGTH || !LOGO.matcher(value).matches()) {
            throw new InvalidMasterDataException("logoDataUrl", "not a supported image");
        }
        return value;
    }
}
