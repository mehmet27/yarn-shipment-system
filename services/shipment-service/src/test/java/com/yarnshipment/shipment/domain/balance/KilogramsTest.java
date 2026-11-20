package com.yarnshipment.shipment.domain.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Kilo girişinin sınırları (KR-04): 3 ondalık, sıfırdan büyük, en çok 100.000 kg; yuvarlama yarım yukarı. */
class KilogramsTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"0.0005, 0.001", "0.00049, 0.000", "-0.0005, -0.001", "1.2344, 1.234", "1.2345, 1.235", "7, 7.000"})
    @DisplayName("Yuvarlama 3 ondalığa yarım yukarı yapılır")
    void shouldRoundHalfUpToGrams(String value, String expected) {
        assertThat(Kilograms.round(new BigDecimal(value)).toPlainString()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({"0.001, 0.001", "' 12.5 ', 12.500", "100000, 100000.000", "99999.999, 99999.999", "1E+2, 100.000"})
    @DisplayName("Geçerli kilo metni 3 ondalıklı kiloya çevrilir")
    void shouldParseValidKilograms(String text, String expected) {
        assertThat(Kilograms.parsePositive(text, "netKg").toPlainString()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" reddedilir")
    @ValueSource(strings = {"0", "0.000", "-1", "0.0005", "1.2345", "100000.001", "1e9", "1,5", "12 kg", "", "NaN"})
    @DisplayName("Sıfır, eksi, 3 ondalıktan hassas, sınırı aşan ya da sayı olmayan kilo alan adıyla reddedilir")
    void shouldRejectInvalidKilograms(String text) {
        assertThatThrownBy(() -> Kilograms.parsePositive(text, "netKg"))
                .isInstanceOf(InvalidLedgerInputException.class)
                .hasMessageContaining("netKg");
    }
}
