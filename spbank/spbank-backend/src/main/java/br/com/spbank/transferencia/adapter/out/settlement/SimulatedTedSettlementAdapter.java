
package br.com.spbank.transferencia.adapter.out.settlement;

import br.com.spbank.transferencia.application.port.out.TedSettlementInstruction;
import br.com.spbank.transferencia.application.port.out.TedSettlementPort;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public final class SimulatedTedSettlementAdapter
        implements TedSettlementPort {

    @Override
    public String settle(
            TedSettlementInstruction instruction
    ) {
        return "TED-SIM-" + UUID.randomUUID();
    }
}
