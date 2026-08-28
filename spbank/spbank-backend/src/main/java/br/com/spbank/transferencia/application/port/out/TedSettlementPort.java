package br.com.spbank.transferencia.application.port.out;

public interface TedSettlementPort {

    String settle(TedSettlementInstruction instruction);
}