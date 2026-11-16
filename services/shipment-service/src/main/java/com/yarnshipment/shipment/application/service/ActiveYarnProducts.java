package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.stock.RecordCentralProductionUseCase.YarnRequest;
import com.yarnshipment.shipment.application.port.out.ledger.CentralStockPort;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.stock.ProductShare;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Merkez stok girişlerinde yalnızca Tanımlar'daki aktif iplikler kullanılır; adlar sunucuda çözülür. */
final class ActiveYarnProducts {

    private final CentralStockPort stock;

    ActiveYarnProducts(CentralStockPort stock) {
        this.stock = stock;
    }

    /** Tanımın adı; tanım yoksa ya da pasifse {@link InvalidLedgerInputException}. */
    String nameOf(UUID productId, String field) {
        String name = stock.findActiveProductNames(List.of(productId)).get(productId);
        if (name == null) {
            throw new InvalidLedgerInputException(field);
        }
        return name;
    }

    List<ProductShare> toShares(List<YarnRequest> yarns) {
        Map<UUID, String> names = stock.findActiveProductNames(
                yarns.stream().map(YarnRequest::productId).toList());
        return yarns.stream()
                .map(yarn -> {
                    String name = names.get(yarn.productId());
                    if (name == null) {
                        throw new InvalidLedgerInputException("yarns.productId");
                    }
                    return new ProductShare(yarn.productId(), name, yarn.netKg());
                })
                .toList();
    }
}
