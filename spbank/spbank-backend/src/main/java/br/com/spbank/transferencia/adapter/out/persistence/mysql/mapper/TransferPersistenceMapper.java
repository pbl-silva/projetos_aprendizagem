package br.com.spbank.transferencia.adapter.out.persistence.mysql.mapper;

import br.com.spbank.transferencia.adapter.out.persistence.mysql.data.TransferData;
import br.com.spbank.transferencia.application.model;
import br.com.spbank.transferencia.application.modelRecipient;

public final class TransferPersistenceMapper {

    private TransferPersistenceMapper() {
    }

    public static Transfer toDomain(
            TransferData data
    ) {

        return new Transfer(
                data.getId(),
                data.getType(),
                data.getSourceAccountId(),
                data.getTargetAccountId(),

                new TransferRecipient(
                        data.getRecipientName(),
                        data.getRecipientDocument(),
                        data.getRecipientBankCode(),
                        data.getRecipientBranch(),
                        data.getRecipientAccountNumber(),
                        data.getRecipientAccountType()
                ),

                data.getAmount(),
                data.getFee(),
                data.isFeeCalculated(),
                data.getIdempotencyKey(),
                data.getRequestFingerprint(),
                data.getStatus(),
                data.getRequestedAt(),
                data.getScheduledFor(),
                data.getProcessedAt(),
                data.getFailureCode(),
                data.getFailureReason(),
                data.getSettlementReference()
        );
    }

    public static TransferData toData(
            Transfer transfer
    ) {

        return new TransferData(
                transfer.getId(),
                transfer.getType(),
                transfer.getSourceAccountId(),
                transfer.getTargetAccountId(),

                transfer.getRecipient().name(),
                transfer.getRecipient().document(),
                transfer.getRecipient().bankCode(),
                transfer.getRecipient().branch(),
                transfer.getRecipient().accountNumber(),
                transfer.getRecipient().accountType(),

                transfer.getAmount(),
                transfer.getFee(),
                transfer.isFeeCalculated(),
                transfer.getIdempotencyKey(),
                transfer.getRequestFingerprint(),
                transfer.getStatus(),
                transfer.getRequestedAt(),
                transfer.getScheduledFor(),
                transfer.getProcessedAt(),
                transfer.getFailureCode(),
                transfer.getFailureMessageKey(),
                transfer.getSettlementReference()
        );
    }
}