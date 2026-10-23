package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;

import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardInstrumentData;
import java.util.*;
import java.time.LocalDate;
import br.com.spbank.cartao.application.model.CardState;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CardInstrumentJpaRepository extends JpaRepository<CardInstrumentData, UUID> {
    List<CardInstrumentData> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    Optional<CardInstrumentData> findByIdAndCustomerId(UUID cardId, UUID customerId);
    // Locking read sees a successor committed while this transaction waited for the customer.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CardInstrumentData> findByPreviousCardId(UUID previousCardId);

    @Query("select c.customerId from CardInstrumentData c where c.id = :cardId")
    Optional<UUID> ownerId(UUID cardId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CardInstrumentData c where c.id = :cardId and c.customerId = :customerId")
    Optional<CardInstrumentData> findForUpdate(UUID customerId, UUID cardId);

    @Query("""
        select c.id from CardInstrumentData c
        where (c.state = :active and c.expiresOn <= :through
            and (c.expiresOn < :today or not exists
                (select s.id from CardInstrumentData s where s.previousCardId = c.id)))
          or (c.state = :pending and c.expiresOn < :today)
        order by c.expiresOn, c.id
        """)
    List<UUID> findLifecycleDueIds(LocalDate today, LocalDate through,
            CardState active, CardState pending, Pageable page);
}
