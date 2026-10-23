package br.com.spbank.cartao.adapter.out.persistence.mysql;

import static org.assertj.core.api.Assertions.*;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardInstrumentData;
import br.com.spbank.cartao.adapter.out.persistence.mysql.repository.CardInstrumentJpaRepository;
import br.com.spbank.cartao.application.model.CardState;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

class CardLifecycleQueryTest {
    @Test void instrumentAndSuccessorQueriesUsePessimisticWriteLocks() throws Exception {
        assertThat(java.util.Objects.requireNonNull(CardInstrumentJpaRepository.class.getMethod("findForUpdate",UUID.class,UUID.class)
            .getAnnotation(Lock.class)).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(java.util.Objects.requireNonNull(CardInstrumentJpaRepository.class.getMethod("findByPreviousCardId",UUID.class)
            .getAnnotation(Lock.class)).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test void hibernateParsesAndTypeChecksTheCandidateQueryWithoutConnectingToADatabase() throws Exception {
        var query=java.util.Objects.requireNonNull(CardInstrumentJpaRepository.class.getMethod("findLifecycleDueIds",LocalDate.class,LocalDate.class,
            CardState.class,CardState.class,Pageable.class).getAnnotation(Query.class)).value();
        var registry=new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect","org.hibernate.dialect.MySQLDialect")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access",false)
            .applySetting("hibernate.hbm2ddl.auto","none").build();
        try(var factory=new MetadataSources(registry).addAnnotatedClass(CardInstrumentData.class).buildMetadata().buildSessionFactory();
                var session=factory.openSession()) {
            var typed=session.createSelectionQuery(query,UUID.class);
            typed.setParameter("today",LocalDate.of(2026,10,23));
            typed.setParameter("through",LocalDate.of(2026,11,22));
            typed.setParameter("active",CardState.ACTIVE);
            typed.setParameter("pending",CardState.PENDING_ACTIVATION);
        } finally {StandardServiceRegistryBuilder.destroy(registry);}
    }
}
