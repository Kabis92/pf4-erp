package com.pf4erp.rh.application.port;

import com.pf4erp.rh.domain.Employe;
import com.pf4erp.rh.domain.EmployeId;

import java.util.List;
import java.util.Optional;

public interface EmployeRepository {

    Optional<Employe> findById(EmployeId id);

    Optional<Employe> findByUserId(String userId);

    List<Employe> findAll();

    Employe save(Employe employe);

    void delete(EmployeId id);

    long count();

    boolean existsByUserId(String userId, EmployeId excludingId);
}
