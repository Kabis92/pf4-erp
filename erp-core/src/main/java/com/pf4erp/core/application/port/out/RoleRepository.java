package com.pf4erp.core.application.port.out;

import com.pf4erp.core.domain.auth.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findById(String id);

    Optional<Role> findByCode(String code);

    List<Role> findAll();

    List<Role> findByCodes(Iterable<String> codes);

    Role save(Role role);

    void deleteById(String id);

    boolean existsByCode(String code);

    long count();
}
