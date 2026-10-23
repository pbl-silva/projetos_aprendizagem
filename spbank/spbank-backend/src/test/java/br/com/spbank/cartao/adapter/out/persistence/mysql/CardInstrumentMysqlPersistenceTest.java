package br.com.spbank.cartao.adapter.out.persistence.mysql;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardInstrumentData;
import br.com.spbank.cartao.adapter.out.persistence.mysql.repository.*;
import br.com.spbank.cartao.application.model.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CardInstrumentMysqlPersistenceTest {
    @Test void persistenceRoundTripPreservesAllIndependentBlocksAndSecretsWithoutExposure() {
        var card=new CardInstrument(UUID.randomUUID(),UUID.randomUUID(),CardBrand.BRISA,UUID.randomUUID(),"Cliente","1234",
            "fingerprint","encrypted-pan","encrypted-cvv","hashed-pin",CardState.ACTIVE,3,true,true,true,"Perda","Análise",
            LocalDate.of(2029,10,31),Instant.parse("2026-10-23T12:00:00Z"));
        var data=new CardInstrumentData(card);
        assertThat(data.toDomain()).isEqualTo(card);
        var repository=java.util.Objects.requireNonNull(mock(CardInstrumentJpaRepository.class));
        var adapter=new CardInstrumentMysqlPersistence(repository,mock(CardBlockAuditJpaRepository.class));
        when(repository.findByIdAndCustomerId(card.id(),card.customerId())).thenReturn(Optional.of(data));
        assertThat(adapter.find(card.customerId(),card.id())).contains(card);
        adapter.save(card);
        java.util.Objects.requireNonNull(verify(repository)).saveAndFlush(argThat(row -> row.toDomain().equals(card)));
    }
}
