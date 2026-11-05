package com.yarnshipment.shipment.masterdata.domain.branding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Marka ayarı doğrulaması: renk #rrggbb, ad kısa, logo yalnızca desteklenen base64 görsel. */
class BrandingTest {

    private static final String LOGO = "data:image/png;base64,iVBORw0KGgo=";

    @Test
    @DisplayName("Renk küçük harfe çevrilir, boş ad ve logo yok sayılır")
    void shouldNormalize() {
        Branding branding = new Branding("  ", "#17527A", " ");

        assertThat(branding.primaryColor()).isEqualTo("#17527a");
        assertThat(branding.companyName()).isNull();
        assertThat(branding.logoDataUrl()).isNull();
    }

    @Test
    @DisplayName("Firma adının kenar boşlukları atılır")
    void shouldTrimName() {
        assertThat(new Branding(" Gencer Fabric ", "#112233", LOGO).companyName())
                .isEqualTo("Gencer Fabric");
    }

    @Test
    @DisplayName("Geçersiz renk reddedilir")
    void shouldRejectBadColor() {
        assertThatThrownBy(() -> new Branding(null, "red", null)).isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> new Branding(null, "#12345", null)).isInstanceOf(InvalidMasterDataException.class);
    }

    @Test
    @DisplayName("Çok uzun ad reddedilir")
    void shouldRejectLongName() {
        assertThatThrownBy(() -> new Branding("a".repeat(Branding.MAX_NAME_LENGTH + 1), "#112233", null))
                .isInstanceOf(InvalidMasterDataException.class);
    }

    @Test
    @DisplayName("Desteklenmeyen logo türü ve fazla büyük logo reddedilir")
    void shouldRejectBadLogo() {
        assertThatThrownBy(() -> new Branding(null, "#112233", "data:text/html;base64,PHNjcmlwdD4="))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> new Branding(null, "#112233", "https://example.com/logo.png"))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() ->
                        new Branding(null, "#112233", "data:image/png;base64," + "A".repeat(Branding.MAX_LOGO_LENGTH)))
                .isInstanceOf(InvalidMasterDataException.class);
    }
}
