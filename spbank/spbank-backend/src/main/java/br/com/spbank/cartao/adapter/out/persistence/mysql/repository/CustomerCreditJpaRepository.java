package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CustomerCreditData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerCreditJpaRepository extends JpaRepository<CustomerCreditData, UUID> {

}
