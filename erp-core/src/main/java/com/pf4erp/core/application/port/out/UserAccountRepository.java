package com.pf4erp.core.application.port.out;

import com.pf4erp.core.domain.auth.UserAccount;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {

    Optional<UserAccount> findById(String id);

    Optional<UserAccount> findByUsername(String username);

    List<UserAccount> findAll();

    UserAccount save(UserAccount user);

    void deleteById(String id);

    boolean existsByUsername(String username);

    long count();
}
