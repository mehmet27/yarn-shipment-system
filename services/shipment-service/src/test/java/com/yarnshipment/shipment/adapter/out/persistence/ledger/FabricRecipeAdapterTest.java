package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.domain.fabric.FabricRecipe;
import com.yarnshipment.shipment.domain.fabric.FabricRecipeConflictException;
import com.yarnshipment.shipment.domain.fabric.FabricRecipeNotFoundException;
import com.yarnshipment.shipment.domain.fabric.FabricRecipeScope;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Kumaş reçetesi kayıtları gerçek PostgreSQL ve uygulama rolünün yetkileriyle denenir. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FabricRecipeAdapter.class)
class FabricRecipeAdapterTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant AT = Instant.parse("2026-11-12T09:00:00Z");

    @Autowired
    private FabricRecipeAdapter recipes;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    private static FabricRecipe recipe(String type, boolean active) {
        return new FabricRecipe(
                UUID.randomUUID(),
                type,
                List.of(
                        new FabricRecipe.Line("30/1 penye", new BigDecimal("60.50")),
                        new FabricRecipe.Line("20/1 open end", new BigDecimal("39.50"))),
                active);
    }

    @Test
    @DisplayName("Reçete karışımıyla kaydedilir, okunur ve pasifleştirilince varsayılan listeden çıkar")
    void shouldStoreReadAndDeactivate() {
        // given
        FabricRecipe supreme = recipe("Süprem", true);
        recipes.insert(supreme, USER, AT);

        // when
        recipes.replace(new FabricRecipe(supreme.id(), "Süprem", supreme.yarns(), false), AT);

        // then
        assertThat(recipes.findAll(FabricRecipeScope.ACTIVE))
                .extracting(FabricRecipe::fabricType)
                .doesNotContain("Süprem");
        assertThat(recipes.findAll(FabricRecipeScope.ALL))
                .filteredOn(found -> found.id().equals(supreme.id()))
                .singleElement()
                .satisfies(found -> {
                    assertThat(found.isActive()).isFalse();
                    assertThat(found.yarns()).hasSize(2);
                    assertThat(found.yarns().getFirst().ratioPct()).isEqualByComparingTo("60.5");
                });
    }

    @Test
    @DisplayName("Aynı kumaş cinsi (büyük/küçük harf fark etmez) ikinci kez eklenemez")
    void shouldRejectDuplicateFabricType() {
        // given
        recipes.insert(recipe("Interlok", true), USER, AT);

        // when / then
        assertThatThrownBy(() -> recipes.insert(recipe("INTERLOK", true), USER, AT))
                .isInstanceOf(FabricRecipeConflictException.class);
    }

    @Test
    @DisplayName("Olmayan reçete değiştirilemez")
    void shouldRejectUnknownRecipe() {
        // when / then
        assertThatThrownBy(() -> recipes.replace(recipe("Ribana", true), AT))
                .isInstanceOf(FabricRecipeNotFoundException.class);
    }
}
