package br.com.spbank.transferencia.application.model.transfer;

public enum TransferStatus {
    SCHEDULED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}