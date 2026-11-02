package com.yarnshipment.bag.adapter.in.web;

import com.yarnshipment.bag.adapter.in.web.api.LabelsApi;
import com.yarnshipment.bag.adapter.in.web.api.model.PrintLabelsRequest;
import com.yarnshipment.bag.application.port.in.PrintLabelsCommand;
import com.yarnshipment.bag.application.port.in.PrintLabelsUseCase;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Etiket basımı; {@code BAG_CREATE} ister. PDF tarayıcıda açılır ve oradan yazdırılır. Method security proxy'si
 * gerektirdiği için final değildir (SEC-02).
 */
@RestController
public class LabelController implements LabelsApi {

    private static final String LABEL_FILE = "cuval-etiketleri.pdf";

    private final PrintLabelsUseCase printLabelsUseCase;
    private final CurrentActorProvider currentActorProvider;

    public LabelController(PrintLabelsUseCase printLabelsUseCase, CurrentActorProvider currentActorProvider) {
        this.printLabelsUseCase = printLabelsUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('BAG_CREATE')")
    public ResponseEntity<Resource> printLabels(PrintLabelsRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        PrintLabelsCommand command =
                new PrintLabelsCommand(actor, List.copyOf(request.getUniqueKeys()), request.getReprintReason());
        byte[] pdf = printLabelsUseCase.printLabels(command);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(LABEL_FILE).build().toString())
                .body(new ByteArrayResource(pdf));
    }
}
