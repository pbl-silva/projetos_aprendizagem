package br.com.spbank.pix.application.port.out;

public interface PixSettlementPort {

    String settle(
        PixSettlementInstruction instruction
    );
}