package br.com.spbank.pix.adapter.out.settlement;

import br.com.spbank.pix.application.port.out.PixSettlementInstruction;
import br.com.spbank.pix.application.port.out.PixSettlementPort;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public final class StudyPixSettlementAdapter
        implements PixSettlementPort {

    private static final DateTimeFormatter DATE =
        DateTimeFormatter
            .ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneOffset.UTC);

    private final Clock clock;

    public StudyPixSettlementAdapter(
        Clock clock
    ) {
        this.clock = clock;
    }

    @Override
    public String settle(
        PixSettlementInstruction instruction
    ) {
        String random =
            UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 9)
                .toUpperCase();

        return "E00090001"
            + DATE.format(clock.instant())
            + random;
    }
}