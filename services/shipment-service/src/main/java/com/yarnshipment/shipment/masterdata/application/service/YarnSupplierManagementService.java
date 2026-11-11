package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ChangeYarnSupplierStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.CreateYarnSupplierUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListYarnSuppliersUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.UpdateYarnSupplierUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.YarnSupplierPort;
import com.yarnshipment.shipment.masterdata.domain.catalog.YarnSupplier;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataText;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** İplik tedarikçisi listesi, ekleme, güncelleme, pasife alma. Yazma yetkisi uçta verilir. */
@Service
public class YarnSupplierManagementService
        implements ListYarnSuppliersUseCase,
                CreateYarnSupplierUseCase,
                UpdateYarnSupplierUseCase,
                ChangeYarnSupplierStatusUseCase {

    private static final String KIND = "YarnSupplier";

    private final YarnSupplierPort suppliers;

    public YarnSupplierManagementService(YarnSupplierPort suppliers) {
        this.suppliers = suppliers;
    }

    @Override
    @Transactional(readOnly = true)
    public MasterDataPage<YarnSupplier> listYarnSuppliers(MasterDataQuery query) {
        return suppliers.findYarnSuppliers(query);
    }

    @Override
    @Transactional
    public YarnSupplier createYarnSupplier(Actor actor, String name) {
        return suppliers.insertYarnSupplier(actor.userId(), MasterDataText.clean("name", name));
    }

    @Override
    @Transactional
    public YarnSupplier updateYarnSupplier(Actor actor, UUID id, String name) {
        return suppliers
                .updateYarnSupplier(actor.userId(), id, MasterDataText.clean("name", name))
                .orElseThrow(() -> new MasterDataNotFoundException(KIND, id));
    }

    @Override
    @Transactional
    public void changeYarnSupplierStatus(Actor actor, UUID id, MasterDataStatus status) {
        if (!suppliers.setYarnSupplierStatus(actor.userId(), id, status)) {
            throw new MasterDataNotFoundException(KIND, id);
        }
    }
}
