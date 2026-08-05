package com.biblioteca.biblioteca_api;

import com.biblioteca.biblioteca_api.entities.Livro;
import com.biblioteca.biblioteca_api.enums.CategoriaLivro;
import com.biblioteca.biblioteca_api.repositories.LivroRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryLivroRepository implements LivroRepository {

    private final Map<Long, Livro> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    @Override
    public Optional<Livro> findById(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(shallowCopy(store.get(id)));
    }

    @Override
    public Livro save(Livro livro) {
        Objects.requireNonNull(livro, "livro não pode ser null");
        if (livro.getId() == null) {
            long id = seq.getAndIncrement();
            livro.setId(id);
        }
        Livro copy = shallowCopy(livro);
        store.put(copy.getId(), copy);
        return shallowCopy(copy);
    }

    @Override
    public List<Livro> findAll() {
        return store.values().stream()
                .map(this::shallowCopy)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) return;
        store.remove(id);
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) return false;
        return store.containsKey(id);
    }

    @Override
    public List<Livro> findByCategoria(CategoriaLivro categoria) {
        if (categoria == null) return Collections.emptyList();
        return store.values().stream()
                .filter(l -> categoria.equals(l.getCategoria()))
                .map(this::shallowCopy)
                .collect(Collectors.toList());
    }

    @Override
    public List<Livro> findByDisponivel(Boolean disponivel) {
        if (disponivel == null) return Collections.emptyList();
        return store.values().stream()
                .filter(l -> Boolean.valueOf(l.getDisponivel()).equals(disponivel))
                .map(this::shallowCopy)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Livro> findByIsbn(String isbn) {
        if (isbn == null) return Optional.empty();
        return store.values().stream()
                .filter(l -> isbn.equals(l.getIsbn()))
                .findFirst()
                .map(this::shallowCopy);
    }

    @Override
    public List<Livro> buscarPorAutor(String autor) {
        if (autor == null || autor.isBlank()) return Collections.emptyList();
        String lower = autor.toLowerCase();
        return store.values().stream()
                .filter(l -> l.getAutor() != null && l.getAutor().toLowerCase().contains(lower))
                .map(this::shallowCopy)
                .collect(Collectors.toList());
    }

    @Override
    public List<Object[]> contarLivrosDisponiveisPorCategoria() {
        Map<CategoriaLivro, Long> counts = store.values().stream()
                .filter(l -> l.getDisponivel() != null && l.getDisponivel())
                .collect(Collectors.groupingBy(
                        Livro::getCategoria,
                        Collectors.counting()
                ));
        return counts.entrySet().stream()
                .map(e -> new Object[]{e.getKey(), e.getValue()})
                .collect(Collectors.toList());
    }

    // --- Métodos herdados de JpaRepository ---
    @Override
    public void flush() {}

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Livro> S saveAndFlush(S entity) {
        return (S) save(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Livro> List<S> saveAllAndFlush(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        entities.forEach(e -> result.add((S) save(e)));
        return result;
    }

    @Override
    public void deleteAllInBatch(Iterable<Livro> entities) {
        entities.forEach(e -> deleteById(e.getId()));
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    public void deleteAllInBatch() {
        store.clear();
    }

    @Deprecated
    @Override
    public Livro getOne(Long id) {
        return findById(id).orElse(null);
    }

    @Deprecated
    @Override
    public Livro getById(Long id) {
        return findById(id).orElse(null);
    }

    @Override
    public Livro getReferenceById(Long id) {
        return findById(id).orElse(null);
    }

    @Override
    public <S extends Livro> List<S> findAll(org.springframework.data.domain.Example<S> example) {
        return Collections.emptyList();
    }

    @Override
    public <S extends Livro> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) {
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Livro> List<S> saveAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        entities.forEach(e -> result.add((S) save(e)));
        return result;
    }

    @Override
    public List<Livro> findAllById(Iterable<Long> ids) {
        List<Livro> result = new ArrayList<>();
        ids.forEach(id -> findById(id).ifPresent(result::add));
        return result;
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void delete(Livro entity) {
        if (entity != null && entity.getId() != null) {
            deleteById(entity.getId());
        }
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends Livro> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    @Override
    public List<Livro> findAll(org.springframework.data.domain.Sort sort) {
        return findAll();
    }

    @Override
    public org.springframework.data.domain.Page<Livro> findAll(org.springframework.data.domain.Pageable pageable) {
        return org.springframework.data.support.PageableExecutionUtils.getPage(
                findAll(),
                pageable,
                this::count
        );
    }

    @Override
    public <S extends Livro> Optional<S> findOne(org.springframework.data.domain.Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends Livro> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) {
        return org.springframework.data.domain.Page.empty();
    }

    @Override
    public <S extends Livro> long count(org.springframework.data.domain.Example<S> example) {
        return 0;
    }

    @Override
    public <S extends Livro> boolean exists(org.springframework.data.domain.Example<S> example) {
        return false;
    }

    @Override
    public <S extends Livro, R> R findBy(org.springframework.data.domain.Example<S> example,
                                         java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    // --- utilitários ---
    private Livro shallowCopy(Livro original) {
        if (original == null) return null;
        Livro c = new Livro();
        c.setId(original.getId());
        c.setTitulo(original.getTitulo());
        c.setAutor(original.getAutor());
        c.setDisponivel(original.getDisponivel());
        c.setCategoria(original.getCategoria());
        c.setIsbn(original.getIsbn());
        return c;
    }

    public void clear() {
        store.clear();
        seq.set(1);
    }

    public void seed(Livro livro) {
        save(livro);
    }
}