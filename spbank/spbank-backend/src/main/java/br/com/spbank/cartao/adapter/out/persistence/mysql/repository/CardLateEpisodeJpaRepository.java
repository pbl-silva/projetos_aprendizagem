package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardLateEpisodeData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardLateEpisodeJpaRepository extends JpaRepository<CardLateEpisodeData,UUID> {
    List<CardLateEpisodeData> findByCustomerId(UUID customerId);
}
