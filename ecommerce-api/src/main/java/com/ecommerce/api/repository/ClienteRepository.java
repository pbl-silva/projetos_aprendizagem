package com.ecommerce.api.repository;

import com.ecommerce.api.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);
    Optional<Cliente> findByCpf(String cpf);
    List<Cliente> findByAtivoTrue();
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);
}
  