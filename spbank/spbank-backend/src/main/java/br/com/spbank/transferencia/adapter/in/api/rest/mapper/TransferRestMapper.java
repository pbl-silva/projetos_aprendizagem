package br.com.spbank.transferencia.adapter.in.api.rest.mapper;

import br.com.spbank.conta.application.port.in.AccountLookup;
import br.com.spbank.transferencia.adapter.in.api.rest.dto.TransferCreationDto;
import br.com.spbank.transferencia.adapter.in.api.rest.dto.TransferPreviewDto;
import br.com.spbank.transferencia.adapter.in.api.rest.dto.TransferReceiptDto;
import br.com.spbank.transferencia.application.model.transfer.Transfer;
import br.com.spbank.transferencia.application.model.transfer.TransferRecipient;
import br.com.spbank.transferencia.application.port.in.CreateTransferCommand;
import br.com.spbank.transferencia.application.port.in.PreviewTransferCommand;
import br.com.spbank.transferencia.application.port.in.TransferPreview;

import java.util.UUID;

public final class TransferRestMapper {

    private TransferRestMapper() {
    }

    public static CreateTransferCommand toCommand(
            UUID source,
            UUID key,
            TransferCreationDto dto
    ) {

        return new CreateTransferCommand(
                source,
                key,
                dto.recipientName(),
                dto.recipientDocument(),
                lookup(dto),
                dto.amount(),
                dto.scheduledFor()
        );
    }

    public static PreviewTransferCommand toPreviewCommand(
            UUID source,
            TransferCreationDto dto
    ) {

        return new PreviewTransferCommand(
                source,
                dto.recipientName(),
                dto.recipientDocument(),
                lookup(dto),
                dto.amount(),
                dto.scheduledFor()
        );
    }

    public static TransferReceiptDto toReceiptDto(
            Transfer transfer
    ) {

        TransferRecipient target =
                transfer.getRecipient();

        return new TransferReceiptDto(
                transfer.getId(),
                transfer.getType(),
                transfer.getStatus(),
                transfer.getAmount(),
                transfer.getFee(),
                target.name(),
                target.bankCode(),
                target.branch(),
                target.accountNumber(),
                target.accountType(),
                transfer.getScheduledFor(),
                transfer.getRequestedAt(),
                transfer.getProcessedAt(),
                transfer.getFailureCode(),
                transfer.getSettlementReference()
        );
    }

    public static TransferPreviewDto toDto(
            TransferPreview preview
    ) {

        return new TransferPreviewDto(
                preview.type(),
                preview.amount(),
                preview.fee(),
                preview.total(),
                preview.effectiveDate(),
                preview.institutionName(),
                preview.remainingFreeTeds()
        );
    }

    private static AccountLookup lookup(
            TransferCreationDto dto
    ) {

        return new AccountLookup(
                normalize(dto.bankCode()),
                normalize(dto.branch()),
                normalize(dto.accountNumber()),
                dto.accountType()
        );
    }

    private static String normalize(
            String value
    ) {

        return value == null
                ? null
                : value.replaceAll(
                        "[^0-9A-Za-z]",
                        ""
                );
    }
}