package com.yarnshipment.reporting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.BalanceMatrixRow;
import com.yarnshipment.reporting.domain.model.OpeningBalanceRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Analiz Bölüm 14 madde 17: açılış bakiyesi aktarıldıktan sonra bakiye matrisi 09.11.2026 tablosuyla (toplam 262.153
 * kg, merkez 173.505 kg) birebir tutar. Rakamlar basılı tablodan, satır ve sütun toplamları da tablonun kendisinden
 * alınmıştır.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BalanceFireReader.class)
@SuppressWarnings("checkstyle:MagicNumber")
class BalanceMatrixAcceptanceTest {

    private static final Instant OPENED_AT = Instant.parse("2026-11-09T00:00:00Z");

    /** Sütunlar: merkez (Gencer) ve fasoncular. */
    private static final String[] HOLDERS = {
        "Gencer",
        "Kırıkhan",
        "Türkuaz",
        "Runy",
        "Öge",
        "Ankar",
        "Ema",
        "Esha",
        "Asmed",
        "Milan",
        "Kırkgeçit",
        "Pak",
        "Küçükkaya",
        "Günsan",
        "Tülücü"
    };

    /** Tablodaki sütun toplamları, HOLDERS sırasıyla. */
    private static final int[] COLUMN_TOTALS = {
        173505, 4630, 17560, 8775, 3215, 2100, 5985, 9010, 9468, 650, 780, 2250, 8410, 7230, 8585
    };

    private static final int GRAND_TOTAL = 262_153;

    /** Satır adı (iplik cinsi), 15 sütunluk kilolar (0 = boş hücre) ve tablodaki satır toplamı. */
    private record Row(String yarnType, List<Integer> kg, int total) {}

    private static final List<Row> TABLE = List.of(
            new Row(
                    "30/1 PENYE (MELİKE)",
                    List.of(10000, 0, 1770, 1750, 890, 0, 1300, 1600, 1500, 200, 0, 1300, 3300, 1400, 5000),
                    30010),
            new Row("30/1 PENYE (ŞİRECİ)", List.of(9300, 0, 1300, 3750, 0, 0, 0, 440, 0, 0, 0, 0, 0, 300, 0), 15090),
            new Row("30/1 KARDE", List.of(420, 0, 0, 0, 0, 0, 1800, 0, 0, 0, 0, 0, 0, 0, 0), 2220),
            new Row("10/1 OE P.P (İLSAN)", List.of(470, 0, 1050, 0, 0, 665, 0, 0, 450, 0, 0, 0, 2000, 3410, 0), 8045),
            new Row("10/1 OE P.P (ŞİRECİ)", List.of(16500, 0, 4500, 0, 0, 0, 0, 4950, 5184, 0, 0, 0, 0, 0, 0), 31134),
            new Row("10/1 OE P.P (ŞİRECİ) ORJ", List.of(1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 1000),
            new Row("10/1 OE P.P (ÖZ PANCAR)", List.of(905, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 905),
            new Row("12/1 OE P.P (İLSAN)", List.of(350, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 350),
            new Row("12/1 OE P.P (ŞİRECİ)", List.of(3700, 0, 860, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 4560),
            new Row("20 DENYE", List.of(950, 430, 0, 0, 0, 0, 25, 0, 0, 0, 0, 0, 0, 0, 0), 1405),
            new Row("30 DENYE", List.of(2100, 0, 260, 0, 0, 0, 0, 0, 200, 170, 0, 0, 0, 0, 0), 2730),
            new Row("40 DENYE", List.of(1600, 0, 330, 215, 645, 85, 300, 300, 520, 80, 0, 260, 500, 200, 85), 5120),
            new Row("70 DENYE", List.of(570, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 570),
            new Row("28/1 VİSKON", List.of(2400, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 2400),
            new Row("28/1 PENYE", List.of(4590, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 4590),
            new Row("36/1 VİSKON", List.of(12200, 2500, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 14700),
            new Row("36/1 PENYE", List.of(19850, 0, 1900, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0), 21850),
            new Row("24/1 PENYE", List.of(14200, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 14200),
            new Row("20/1 OE", List.of(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3000), 3000),
            new Row("20/1 PENYE", List.of(17800, 0, 2600, 0, 0, 0, 0, 0, 0, 200, 0, 0, 840, 0, 0), 21440),
            new Row(
                    "150 DENYE BEYAZ",
                    List.of(29000, 0, 1060, 960, 960, 750, 1000, 1240, 264, 0, 0, 450, 960, 960, 0),
                    37604),
            new Row("150 DENYE SİYAH", List.of(16000, 0, 790, 2100, 720, 0, 800, 480, 1050, 0, 0, 240, 0, 0, 0), 22180),
            new Row(
                    "75 DENYE BEYAZ",
                    List.of(9600, 1700, 1140, 0, 0, 600, 660, 0, 300, 0, 780, 0, 810, 960, 500),
                    17050));

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private BalanceFireReader reader;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Tablonun kendi toplamları tutarlıdır: satırlar, sütunlar ve genel toplam 262.153")
    void shouldHaveConsistentSourceTable() {
        assertThat(TABLE).allSatisfy(row -> {
            assertThat(row.kg()).hasSize(HOLDERS.length);
            assertThat(row.kg().stream().mapToInt(Integer::intValue).sum()).isEqualTo(row.total());
        });
        for (int col = 0; col < HOLDERS.length; col++) {
            int index = col;
            assertThat(TABLE.stream().mapToInt(row -> row.kg().get(index)).sum())
                    .isEqualTo(COLUMN_TOTALS[col]);
        }
        assertThat(Arrays.stream(COLUMN_TOTALS).sum()).isEqualTo(GRAND_TOTAL);
    }

    @Test
    @DisplayName("Açılış bakiyesi aktarılınca matris 09.11.2026 tablosuyla hücre hücre tutar")
    void shouldMatchTableAfterOpeningImport() {
        UUID[] holderIds = new UUID[HOLDERS.length];
        for (int col = 0; col < HOLDERS.length; col++) {
            holderIds[col] = UUID.randomUUID();
            new SeedRows(jdbc).subcontractor(holderIds[col], HOLDERS[col]);
        }
        for (Row row : TABLE) {
            for (int col = 0; col < HOLDERS.length; col++) {
                if (row.kg().get(col) > 0) {
                    OpeningBalanceRow opening = new OpeningBalanceRow(
                            holderIds[col],
                            HOLDERS[col],
                            row.yarnType(),
                            BigDecimal.valueOf(row.kg().get(col)));
                    assertThat(reader.insertOpeningIfAbsent(opening, OPENED_AT, UUID.randomUUID()))
                            .isTrue();
                }
            }
        }

        List<BalanceMatrixRow> matrix =
                reader.findBalanceMatrix(new ReportScope(Set.of(UUID.randomUUID()), true, null)).stream()
                        .filter(row -> List.of(holderIds).contains(row.subcontractorId()))
                        .toList();

        Map<String, BigDecimal> cells = new HashMap<>();
        matrix.forEach(row -> cells.put(row.yarnType() + "|" + row.subcontractorName(), row.balanceKg()));
        int nonEmptyCells = 0;
        for (Row row : TABLE) {
            for (int col = 0; col < HOLDERS.length; col++) {
                if (row.kg().get(col) > 0) {
                    nonEmptyCells++;
                    assertThat(cells.get(row.yarnType() + "|" + HOLDERS[col]))
                            .as("%s / %s", row.yarnType(), HOLDERS[col])
                            .isEqualByComparingTo(BigDecimal.valueOf(row.kg().get(col)));
                }
            }
        }
        assertThat(matrix).hasSize(nonEmptyCells);
        assertThat(total(matrix, "")).isEqualByComparingTo(BigDecimal.valueOf(GRAND_TOTAL));
        assertThat(total(matrix, "Gencer")).isEqualByComparingTo(BigDecimal.valueOf(COLUMN_TOTALS[0]));
    }

    private static BigDecimal total(List<BalanceMatrixRow> matrix, String holder) {
        return matrix.stream()
                .filter(row -> holder.isEmpty() || holder.equals(row.subcontractorName()))
                .map(BalanceMatrixRow::balanceKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
