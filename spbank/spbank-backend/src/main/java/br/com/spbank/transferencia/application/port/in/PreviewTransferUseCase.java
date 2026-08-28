package br.com.spbank.transferencia.application.port.in;

public interface PreviewTransferUseCase {

    TransferPreview preview(
            PreviewTransferCommand command
    );
}