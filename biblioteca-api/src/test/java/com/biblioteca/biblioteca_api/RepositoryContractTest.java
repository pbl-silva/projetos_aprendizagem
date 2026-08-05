package com.biblioteca.biblioteca_api;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de contrato que valida comportamento mínimo esperado de um repository.
 * Implementações concretas devem prover os métodos abstratos para criar/limpar dados.
 *
 * Obs: este teste é genérico e depende de subclasses fornecerem factory/cleanup.
 */
public abstract class RepositoryContractTest<ID, T> {

    protected abstract void clearRepository();
    protected abstract T createNewEntity();
    protected abstract T saveEntity(T entity);
    protected abstract Optional<T> findById(ID id);
    protected abstract ID idOf(T entity);
    protected abstract T changeEntityForUpdate(T entity);

    @Test
    void findByIdShouldReturnOptionalNotNullWhenNotFound() {
        clearRepository();
        Optional<T> result = findById(null);
        assertNotNull(result, "findById must never return null (use Optional.empty())");
    }

    @Test
    void saveShouldReturnEntityNotNull() {
        clearRepository();
        T entity = createNewEntity();
        T saved = saveEntity(entity);
        assertNotNull(saved, "save must not return null");
        assertNotNull(idOf(saved), "saved entity must have an id set according to repository contract");
    }

    @Test
    void saveThenFindByIdShouldReturnSavedEntity() {
        clearRepository();
        T entity = createNewEntity();
        T saved = saveEntity(entity);
        ID id = idOf(saved);
        assertNotNull(id, "id must be present after save");
        Optional<T> fetched = findById(id);
        assertTrue(fetched != null, "findById must not return null");
        assertTrue(fetched.isPresent(), "findById must return Optional.present for saved id");
    }

    @Test
    void updateShouldPersistChanges() {
        clearRepository();
        T entity = createNewEntity();
        T saved = saveEntity(entity);
        T modified = changeEntityForUpdate(saved);
        T updated = saveEntity(modified);
        ID id = idOf(updated);
        Optional<T> fetched = findById(id);
        assertTrue(fetched.isPresent());
        assertEquals(id, idOf(fetched.get()));
    }
}