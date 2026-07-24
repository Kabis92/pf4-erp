package com.pf4erp.rh.config;

import com.pf4erp.rh.adapters.out.persistence.mongo.EmployeDocument;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Seed démo : lie le user « user » à un employé si absent.
 */
public class RhDemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(RhDemoDataSeeder.class);

    private final MongoTemplate mongoTemplate;

    public RhDemoDataSeeder(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void seed() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> userDoc = mongoTemplate.findOne(
                    Query.query(Criteria.where("username").is("user")),
                    Map.class,
                    "users"
            );
            if (userDoc == null || userDoc.get("_id") == null) {
                return;
            }
            String userId = String.valueOf(userDoc.get("_id"));
            boolean linked = mongoTemplate.exists(
                    Query.query(Criteria.where("userId").is(userId)),
                    EmployeDocument.class
            );
            if (linked) {
                return;
            }
            EmployeDocument employe = new EmployeDocument();
            employe.setId(UUID.randomUUID().toString());
            employe.setNom("Utilisateur RH");
            employe.setEmail("user@erp.local");
            employe.setPoste("Collaborateur");
            employe.setDateEmbauche(LocalDate.now().minusMonths(6));
            employe.setUserId(userId);
            mongoTemplate.save(employe);
            log.info("Seeded demo employé linked to user {}", userId);
        } catch (Exception e) {
            log.warn("RH demo seed skipped: {}", e.getMessage());
        }
    }
}
