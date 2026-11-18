package com.yarnshipment.shipment.application.service.receipt;

import com.yarnshipment.shipment.application.port.in.stock.RecordYarnReceiptUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.CentralStockPort;
import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.application.port.out.ledger.YarnLotPort;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import com.yarnshipment.shipment.domain.stock.LotRequest;
import com.yarnshipment.shipment.domain.stock.ReceiptDetails;
import com.yarnshipment.shipment.domain.stock.ResolvedLot;
import com.yarnshipment.shipment.domain.stock.YarnLotNo;
import java.time.Clock;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merkeze iplik girişi: iplik ve tedarikçi Tanımlar'daki aktif kayıtlardan seçilir; parti numarası yazılmışsa
 * Partiler'deki partiye bağlanır (yoksa açılır), böylece sevkiyat ve iplik dönüşündeki parti seçimi girişten beslenir.
 */
@Service
public class YarnReceiptService implements RecordYarnReceiptUseCase {

    private final CentralStockPort stock;
    private final YarnLotPort lots;
    private final LedgerEventPort events;
    private final Clock clock;

    public YarnReceiptService(CentralStockPort stock, YarnLotPort lots, LedgerEventPort events, Clock clock) {
        this.stock = stock;
        this.lots = lots;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CentralStockMovement recordYarnReceipt(RecordYarnReceiptUseCase.Command command) {
        String yarnName =
                stock.findActiveProductNames(List.of(command.productId())).get(command.productId());
        if (yarnName == null) {
            throw new InvalidLedgerInputException("productId");
        }
        String supplierName = stock.findActiveSupplierName(command.supplierId())
                .orElseThrow(() -> new InvalidLedgerInputException("supplierId"));
        ResolvedLot lot = resolveLot(command);
        CentralStockMovement receipt = CentralStockMovement.receipt(
                new CentralStockMovement.Receipt(command.productId(), yarnName, command.netKg(), command.note()),
                clock.instant(),
                command.actor().userId());
        ReceiptDetails details = new ReceiptDetails(
                command.supplierId(),
                supplierName,
                lot == null ? null : lot.id(),
                lot == null ? null : lot.lotNo(),
                command.waybillNo());
        stock.saveReceipt(receipt, details);
        events.yarnReceiptRecorded(receipt);
        return receipt;
    }

    private @Nullable ResolvedLot resolveLot(RecordYarnReceiptUseCase.Command command) {
        if (command.lotNo() == null || command.lotNo().isBlank()) {
            return null;
        }
        return lots.resolve(new LotRequest(
                command.actor().userId(),
                command.actor().siteIds(),
                command.productId(),
                command.supplierId(),
                YarnLotNo.normalize(command.lotNo()),
                command.qualityGradeId()));
    }
}
