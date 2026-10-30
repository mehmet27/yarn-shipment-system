package com.yarnshipment.shipment.masterdata.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CatalogRulesTest {

    @Test
    @DisplayName("Kod boşluklardan arındırılır ve büyük harfe çevrilir")
    void shouldNormalizeCode() {
        assertThat(MasterDataCode.normalize("code", "  pes-30/1 ")).isEqualTo("PES-30/1");
        assertThat(MasterDataCode.normalize("code", "a+")).isEqualTo("A+");
    }

    @Test
    @DisplayName("Boş, boşluklu, Türkçe harfli ya da 40 karakteri aşan kod reddedilir")
    void shouldRejectInvalidCode() {
        assertThatThrownBy(() -> MasterDataCode.normalize("code", "  ")).isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> MasterDataCode.normalize("code", "PES 30"))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> MasterDataCode.normalize("lotNo", "ŞÖ-1"))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> MasterDataCode.normalize("code", "-A")).isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> MasterDataCode.normalize("code", "A".repeat(MasterDataCode.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidMasterDataException.class);
    }

    @Test
    @DisplayName("Etiket rengi büyük harfli #RRGGBB olur; boş renk yok sayılır, biçimsiz renk reddedilir")
    void shouldValidateLabelColor() {
        assertThat(new QualityGradeDetails("Birinci", "#2e7d32").labelColor()).isEqualTo("#2E7D32");
        assertThat(new QualityGradeDetails("Birinci", " ").labelColor()).isNull();
        assertThatThrownBy(() -> new QualityGradeDetails("Birinci", "red"))
                .isInstanceOf(InvalidMasterDataException.class)
                .extracting(e -> ((InvalidMasterDataException) e).field())
                .isEqualTo("labelColor");
    }

    @Test
    @DisplayName("İplik numarası isteğe bağlıdır; boşsa yok sayılır, 50 karakteri aşamaz")
    void shouldCleanYarnCount() {
        assertThat(new ProductDetails(" Polyester ", "  ").yarnCount()).isNull();
        assertThat(new ProductDetails("Polyester", " Ne 30/1 ").yarnCount()).isEqualTo("Ne 30/1");
        assertThatThrownBy(() -> new ProductDetails("Polyester", "x".repeat(OptionalText.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidMasterDataException.class);
    }
}
