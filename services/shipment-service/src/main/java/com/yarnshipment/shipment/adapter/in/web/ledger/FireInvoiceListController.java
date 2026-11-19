package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.ledger.FireInvoiceListViews.InvoicePageView;
import com.yarnshipment.shipment.application.port.in.fire.ListFireInvoicesQuery;
import com.yarnshipment.shipment.application.port.in.fire.ListFireInvoicesUseCase;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Faturalananlar listesi ucu. Method security proxy'si gerektirdiği için final değildir. */
@RestController
public class FireInvoiceListController {

    private final ListFireInvoicesUseCase listFireInvoicesUseCase;
    private final CurrentActorProvider currentActorProvider;

    public FireInvoiceListController(
            ListFireInvoicesUseCase listFireInvoicesUseCase, CurrentActorProvider currentActorProvider) {
        this.listFireInvoicesUseCase = listFireInvoicesUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @GetMapping("/api/v1/fire-invoices")
    @PreAuthorize("hasAuthority('BALANCE_VIEW')")
    public ResponseEntity<InvoicePageView> listFireInvoices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to,
            @RequestParam(required = false) @Nullable UUID subcontractorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidLedgerInputException("to");
        }
        ListFireInvoicesQuery query = new ListFireInvoicesQuery(
                currentActorProvider.getCurrentActor(), from, to, subcontractorId, page, size);
        return ResponseEntity.ok(FireInvoiceListViews.of(listFireInvoicesUseCase.listFireInvoices(query)));
    }
}
