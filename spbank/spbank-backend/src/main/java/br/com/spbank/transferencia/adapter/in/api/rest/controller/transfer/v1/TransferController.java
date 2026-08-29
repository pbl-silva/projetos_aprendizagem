package br.com.spbank.transferencia.adapter.in.api.rest.controller.transfer.v1;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ACCOUNT_ID;
import static br.com.spbank.transferencia.adapter.in.api.rest.mapper.TransferRestMapper.*;

import br.com.spbank.autenticacao.application.port.in.AuthenticationUseCase;
import br.com.spbank.transferencia.adapter.in.api.rest.dto.TransferConfirmationDto;
import br.com.spbank.transferencia.adapter.in.api.rest.dto.TransferCreationDto;
import br.com.spbank.transferencia.adapter.in.api.rest.dto.TransferPreviewDto;
import br.com.spbank.transferencia.adapter.in.api.rest.dto.TransferReceiptDto;
import br.com.spbank.transferencia.application.model.transfer.Transfer;
import br.com.spbank.transferencia.application.port.in.CancelTransferUseCase;
import br.com.spbank.transferencia.application.port.in.CreateTransferUseCase;
import br.com.spbank.transferencia.application.port.in.GetTransferUseCase;
import br.com.spbank.transferencia.application.port.in.ListScheduledTransfersUseCase;
import br.com.spbank.transferencia.application.port.in.PreviewTransferUseCase;

import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public final class TransferController {

    private final CreateTransferUseCase create;
    private final PreviewTransferUseCase preview;
    private final GetTransferUseCase query;
    private final CancelTransferUseCase cancel;
    private final AuthenticationUseCase authentication;
    private final ListScheduledTransfersUseCase scheduledTransfers;

    public TransferController(
            CreateTransferUseCase create,
            PreviewTransferUseCase preview,
            GetTransferUseCase query,
            CancelTransferUseCase cancel,
            AuthenticationUseCase authentication,
            ListScheduledTransfersUseCase scheduledTransfers
    ) {
        this.create = create;
        this.preview = preview;
        this.query = query;
        this.cancel = cancel;
        this.authentication = authentication;
        this.scheduledTransfers = scheduledTransfers;
    }

    @PostMapping("/preview")
    public TransferPreviewDto preview(
            @RequestAttribute(ACCOUNT_ID) UUID source,
            @Valid @RequestBody TransferCreationDto dto
    ) {

        return toDto(
                preview.preview(
                        toPreviewCommand(
                                source,
                                dto
                        )
                )
        );
    }

    @PostMapping
    public ResponseEntity<TransferReceiptDto> create(
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestAttribute(ACCOUNT_ID) UUID source,
            @Valid @RequestBody TransferConfirmationDto confirmation
    ) {

        authentication.confirmPassword(
                source,
                confirmation.confirmationPassword()
        );

        TransferReceiptDto receipt = receipt(
                create.create(
                        toCommand(
                                source,
                                key,
                                confirmation.transfer()
                        )
                )
        );

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/v1/transfers/"
                                        + receipt.id()
                        )
                )
                .body(receipt);
    }

    @GetMapping("/{id}")
    public TransferReceiptDto get(
            @PathVariable UUID id,
            @RequestAttribute(ACCOUNT_ID) UUID source
    ) {

        return receipt(
                query.get(
                        id,
                        source
                )
        );
    }

@GetMapping("/scheduled")
public List<TransferReceiptDto> scheduled(
        @RequestAttribute(ACCOUNT_ID) UUID source
) {

    return scheduledTransfers
            .list(source)
            .stream()
            .map(transfer -> toReceiptDto(transfer))
            .toList();
}

    @DeleteMapping("/{id}")
    public TransferReceiptDto cancel(
            @PathVariable UUID id,
            @RequestAttribute(ACCOUNT_ID) UUID source
    ) {

        return receipt(
                cancel.cancel(
                        id,
                        source
                )
        );
    }

    private TransferReceiptDto receipt(
            Transfer transfer
    ) {

        return toReceiptDto(transfer);
    }
}