package com.pf4erp.rh.application;

import com.pf4erp.rh.application.port.EmployeRepository;
import com.pf4erp.rh.domain.Employe;
import com.pf4erp.rh.domain.EmployeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmployeUseCaseTest {

    private EmployeUseCase useCase;

    @BeforeEach
    void setUp() {
        ApplicationEventPublisher noopPublisher = event -> {
        };
        useCase = new EmployeUseCase(new FakeEmployeRepository(), noopPublisher);
    }

    @Test
    void createAndList() {
        Employe created = useCase.create(
                "Ada Lovelace", "ada@example.com", null, "Ingénieure", LocalDate.of(2024, 1, 15), null, null
        );
        assertEquals(1, useCase.list().size());
        assertEquals("Ada Lovelace", useCase.get(created.getId()).getNom());
        assertEquals(1, useCase.count());
    }

    @Test
    void update() {
        Employe created = useCase.create("Ada", "ada@example.com", "+1 555", "Dev", LocalDate.of(2024, 1, 1), "u1", null);
        Employe updated = useCase.update(
                created.getId(), "Ada L.", "ada@example.com", "+1 555", "Lead", LocalDate.of(2024, 1, 1), "u1", null
        );
        assertEquals("Lead", updated.getPoste());
        assertEquals("u1", updated.getUserId());
    }

    @Test
    void getMissing() {
        assertThrows(EmployeUseCase.EmployeNotFoundException.class,
                () -> useCase.get(EmployeId.of("missing")));
    }

    @Test
    void rejectDuplicateUserLink() {
        useCase.create("Ada", "ada@example.com", null, "Dev", LocalDate.of(2024, 1, 1), "u1", null);
        assertThrows(EmployeUseCase.UserAlreadyLinkedException.class,
                () -> useCase.create("Bob", "bob@example.com", null, "Dev", LocalDate.of(2024, 1, 1), "u1", null));
    }

    static class FakeEmployeRepository implements EmployeRepository {
        private final Map<String, Employe> store = new ConcurrentHashMap<>();

        @Override
        public Optional<Employe> findById(EmployeId id) {
            return Optional.ofNullable(store.get(id.getValue()));
        }

        @Override
        public Optional<Employe> findByUserId(String userId) {
            return store.values().stream()
                    .filter(e -> userId.equals(e.getUserId()))
                    .findFirst();
        }

        @Override
        public List<Employe> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public Employe save(Employe employe) {
            store.put(employe.getId().getValue(), employe);
            return employe;
        }

        @Override
        public void delete(EmployeId id) {
            store.remove(id.getValue());
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public boolean existsByUserId(String userId, EmployeId excludingId) {
            return store.values().stream()
                    .anyMatch(e -> userId.equals(e.getUserId())
                            && (excludingId == null || !e.getId().equals(excludingId)));
        }
    }
}
