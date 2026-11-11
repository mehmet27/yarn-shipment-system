package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecipeLineOrderFormTest {

    private static RecipeLine orderLine(String price) {
        return new RecipeLine(
                1,
                null,
                "iplik",
                null,
                new BigDecimal("3850"),
                " 30/150 Penye Beyaz ",
                "30 denye",
                WidthType.OPEN_WIDTH,
                "100/31-11-11",
                "32/28",
                price == null ? null : new BigDecimal(price));
    }

    @Test
    @DisplayName("Sipariş formu alanları kırpılır ve fiyat dört ondalığa tamamlanır")
    void shouldNormalizeOrderFormFields() {
        RecipeLine line = orderLine("21");

        assertThat(line.fabricType()).isEqualTo("30/150 Penye Beyaz");
        assertThat(line.unitPrice()).isEqualByComparingTo("21.0000");
        assertThat(line.unitPrice().scale()).isEqualTo(4);
    }

    @Test
    @DisplayName("Sipariş formu alanları olmadan oluşturulan satırda hepsi boştur")
    void shouldAllowLineWithoutOrderFormFields() {
        RecipeLine line = new RecipeLine(1, null, "iplik", null, new BigDecimal("10"));

        assertThat(line.fabricType()).isNull();
        assertThat(line.widthType()).isNull();
        assertThat(line.unitPrice()).isNull();
    }

    @Test
    @DisplayName("Negatif fiyat geçersizdir")
    void shouldRejectNegativePrice() {
        assertThatThrownBy(() -> orderLine("-1")).isInstanceOf(InvalidNewShipmentException.class);
    }

    @Test
    @DisplayName("Ölçeklenen satır sipariş formu alanlarını korur")
    void shouldKeepOrderFormFieldsWhenScaled() {
        RecipeLine scaled = orderLine("21").withNetKg(new BigDecimal("100"));

        assertThat(scaled.pussFine()).isEqualTo("32/28");
        assertThat(scaled.widthType()).isEqualTo(WidthType.OPEN_WIDTH);
        assertThat(scaled.netKg()).isEqualByComparingTo("100");
    }
}
