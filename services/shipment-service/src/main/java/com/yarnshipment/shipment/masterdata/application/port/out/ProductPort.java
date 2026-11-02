package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.catalog.Product;
import com.yarnshipment.shipment.masterdata.domain.catalog.ProductDetails;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.Optional;
import java.util.UUID;

/**
 * İplik okuma ve yazma portu. Liste koda göre sıralıdır, arama kod ve adda yapılır. Yazmalar değişikliği yapan
 * kullanıcıyı transaction'a işler (audit); kullanılmış kod {@code DuplicateCodeException} ile reddedilir.
 */
public interface ProductPort {

    MasterDataPage<Product> findProducts(MasterDataQuery query);

    Product insertProduct(UUID actorId, String code, ProductDetails details);

    /** Güncellenen kaydı döner; kayıt yoksa boş. */
    Optional<Product> updateProduct(UUID actorId, UUID id, ProductDetails details);

    /** Kayıt yoksa {@code false}. */
    boolean setProductStatus(UUID actorId, UUID id, MasterDataStatus status);
}
