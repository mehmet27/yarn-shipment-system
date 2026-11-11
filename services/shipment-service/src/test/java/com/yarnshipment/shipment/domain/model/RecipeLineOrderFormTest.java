package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecipeLineOrderFormTest {

    private static final int PRICE_SCALE = 4;

    private static RecipeLine orderLine(String price) {
        OrderSpec spec = new OrderSpec(
                " 30/150 Penye Beyaz ",
                "30 denye",
                WidthType.OPEN_WIDTH,
                "100/31-11-11",
                "32/28",
                price == null ? null : new BigDecimal(price));
        return new RecipeLine(1, null, "iplik", null, new BigDecimal("3850"), spec);
    }

    @Test
    @DisplayName("Sipariş formu alanları kırpılır ve fiyat dört ondalığa tamamlanır")
    void shouldNormalizeOrderFormFields() {
        RecipeLine line = orderLine("21");

        assertThat(line.orderSpec().fabricType()).isEqualTo("30/150 Penye Beyaz");
        assertThat(line.orderSpec().unitPrice()).isEqualByComparingTo("21.0000");
        assertThat(line.orderSpec().unitPrice().scale()).isEqualTo(PRICE_SCALE);
    }

    @Test
    @DisplayName("Sipariş formu alanları olmadan oluşturulan satırda hepsi boştur")
    void shouldAllowLineWithoutOrderFormFields() {
        RecipeLine line = new RecipeLine(1, null, "iplik", null, new BigDecimal("10"));

        assertThat(line.orderSpec().fabricType()).isNull();
        assertThat(line.orderSpec().widthType()).isNull();
        assertThat(line.orderSpec().unitPrice()).isNull();
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

        assertThat(scaled.orderSpec().pussFine()).isEqualTo("32/28");
        assertThat(scaled.orderSpec().widthType()).isEqualTo(WidthType.OPEN_WIDTH);
        assertThat(scaled.netKg()).isEqualByComparingTo("100");
    }
}
