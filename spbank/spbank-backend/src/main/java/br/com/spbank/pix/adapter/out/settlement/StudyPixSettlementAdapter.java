package br.com.spbank.pix.adapter.out.settlement;

import br.com.spbank.pix.application.model.PixPayment;
import br.com.spbank.pix.application.port.out.PixSettlementPort;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public final class StudyPixSettlementAdapter
        implements PixSettlementPort {

    @Override
    public String settle(
        PixPayment payment
    ) {
        return "PIX-STUDY-"
            + UUID.randomUUID();
    }
}