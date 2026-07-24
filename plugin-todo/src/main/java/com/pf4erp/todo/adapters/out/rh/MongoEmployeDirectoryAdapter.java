package com.pf4erp.todo.adapters.out.rh;

import com.pf4erp.todo.application.port.EmployeDirectoryPort;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Optional;

/**
 * Lecture seule de la collection RH sans dépendance Java sur plugin-rh.
 */
public class MongoEmployeDirectoryAdapter implements EmployeDirectoryPort {

    private static final String COLLECTION = "rh_employes";

    private final MongoTemplate mongoTemplate;

    public MongoEmployeDirectoryAdapter(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<String> findEmployeIdByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        Document doc = mongoTemplate.findOne(
                Query.query(Criteria.where("userId").is(userId)),
                Document.class,
                COLLECTION
        );
        if (doc == null || doc.get("_id") == null) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(doc.get("_id")));
    }

    @Override
    public Optional<String> findEmployeNameById(String employeId) {
        if (employeId == null || employeId.isBlank()) {
            return Optional.empty();
        }
        Document doc = mongoTemplate.findById(employeId, Document.class, COLLECTION);
        if (doc == null) {
            return Optional.empty();
        }
        Object nom = doc.get("nom");
        return Optional.ofNullable(nom != null ? nom.toString() : null);
    }
}
