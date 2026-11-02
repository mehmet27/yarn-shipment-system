package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.masterdata.domain.catalog.DuplicateCodeException;
import com.yarnshipment.shipment.masterdata.domain.catalog.ProductDetails;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** İplik yazmaları gerçek PostgreSQL'de: kod tekilliği, değişmeyen kod ve audit izi. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProductAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProductAdapterTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ProductAdapter products;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
    }

    @Test
    @DisplayName("Kod büyük/küçük harf duyarsız tektir; güncelleme kodu değiştirmez ve audit'e aktörle düşer")
    void shouldKeepCodesUniqueAndAuditChanges() {
        // given
        UUID actor = UUID.randomUUID();
        var product = inTx(() -> products.insertProduct(actor, "PES-A", new ProductDetails("Polyester", null)));

        // when
        var updated = inTx(() -> products.updateProduct(actor, product.id(), new ProductDetails("Poli", "Ne 30/1")));

        // then
        assertThat(updated).hasValueSatisfying(p -> {
            assertThat(p.code()).isEqualTo("PES-A");
            assertThat(p.details().yarnCount()).isEqualTo("Ne 30/1");
        });
        assertThatThrownBy(() -> inTx(() -> products.insertProduct(actor, "pes-a", new ProductDetails("X", null))))
                .isInstanceOf(DuplicateCodeException.class);
        assertThat(inTx(() -> products.updateProduct(actor, UUID.randomUUID(), new ProductDetails("X", null))))
                .isEmpty();
        assertThat(DATABASE.migratorJdbc()
                        .queryForObject(
                                "SELECT count(*) FROM audit_local WHERE table_name = 'md_product' AND row_id = ?"
                                        + " AND actor_id = ?",
                                Long.class,
                                product.id(),
                                actor.toString()))
                .isEqualTo(2L);
    }

    private <T> T inTx(Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }
}
