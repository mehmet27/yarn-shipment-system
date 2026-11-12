package com.yarnshipment.shipment.masterdata.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CatalogRulesTest {

    private static final long LOT_COUNT = 3;

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
    @DisplayName("Sıra 0-9999 aralığında olur; aralık dışı reddedilir")
    void shouldValidateSortOrder() {
        assertThat(new QualityGradeDetails("Birinci", 0).sortOrder()).isZero();
        assertThat(new QualityGradeDetails("Birinci", QualityGradeDetails.MAX_SORT_ORDER).sortOrder())
                .isEqualTo(QualityGradeDetails.MAX_SORT_ORDER);
        assertThatThrownBy(() -> new QualityGradeDetails("Birinci", -1))
                .isInstanceOf(InvalidMasterDataException.class)
                .extracting(e -> ((InvalidMasterDataException) e).field())
                .isEqualTo("sortOrder");
        assertThatThrownBy(() -> new QualityGradeDetails("Birinci", QualityGradeDetails.MAX_SORT_ORDER + 1))
                .isInstanceOf(InvalidMasterDataException.class);
    }

    @Test
    @DisplayName("İplik numarası isteğe bağlıdır; boşsa yok sayılır, 50 karakteri aşamaz")
    void shouldCleanYarnCount() {
        assertThat(new ProductDetails(" Polyester ", "  ").yarnCount()).isNull();
        assertThat(new ProductDetails("Polyester", " Ne 30/1 ").yarnCount()).isEqualTo("Ne 30/1");
        assertThatThrownBy(() -> new ProductDetails("Polyester", "x".repeat(OptionalText.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidMasterDataException.class);
    }

    @Test
    @DisplayName("İplik ve kalite sınıfı isteğe bağlı alan olmadan oluşur; tekrarlı kod alanını taşır")
    void shouldBuildRecordsWithoutOptionalFields() {
        UUID id = UUID.randomUUID();

        Product product = new Product(id, "PES-30-1", new ProductDetails("Polyester", null), true);
        QualityGrade grade = new QualityGrade(id, "A", new QualityGradeDetails("Birinci", 1), false, LOT_COUNT);

        assertThat(product.details().yarnCount()).isNull();
        assertThat(grade.lotCount()).isEqualTo(LOT_COUNT);
        assertThat(grade.active()).isFalse();
        assertThat(new DuplicateCodeException("lotNo", "P-1").field()).isEqualTo("lotNo");
    }
}
