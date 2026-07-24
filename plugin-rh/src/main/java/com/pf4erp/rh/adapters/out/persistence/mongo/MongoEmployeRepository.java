package com.pf4erp.rh.adapters.out.persistence.mongo;

import com.pf4erp.rh.application.port.EmployeRepository;
import com.pf4erp.rh.domain.Employe;
import com.pf4erp.rh.domain.EmployeId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

public class MongoEmployeRepository implements EmployeRepository {

    private final MongoTemplate mongoTemplate;

    public MongoEmployeRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<Employe> findById(EmployeId id) {
        EmployeDocument doc = mongoTemplate.findById(id.getValue(), EmployeDocument.class);
        return Optional.ofNullable(doc).map(this::toDomain);
    }

    @Override
    public Optional<Employe> findByUserId(String userId) {
        Query query = Query.query(Criteria.where("userId").is(userId));
        return Optional.ofNullable(mongoTemplate.findOne(query, EmployeDocument.class)).map(this::toDomain);
    }

    @Override
    public List<Employe> findAll() {
        return mongoTemplate.findAll(EmployeDocument.class).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Employe save(Employe employe) {
        EmployeDocument doc = toDocument(employe);
        mongoTemplate.save(doc);
        return toDomain(doc);
    }

    @Override
    public void delete(EmployeId id) {
        Query query = Query.query(Criteria.where("_id").is(id.getValue()));
        mongoTemplate.remove(query, EmployeDocument.class);
    }

    @Override
    public long count() {
        return mongoTemplate.count(new Query(), EmployeDocument.class);
    }

    @Override
    public boolean existsByUserId(String userId, EmployeId excludingId) {
        Criteria criteria = Criteria.where("userId").is(userId);
        if (excludingId != null) {
            criteria = criteria.and("_id").ne(excludingId.getValue());
        }
        return mongoTemplate.exists(Query.query(criteria), EmployeDocument.class);
    }

    private Employe toDomain(EmployeDocument doc) {
        return new Employe(
                EmployeId.of(doc.getId()),
                doc.getNom(),
                doc.getEmail(),
                doc.getPoste(),
                doc.getDateEmbauche(),
                doc.getUserId()
        );
    }

    private EmployeDocument toDocument(Employe employe) {
        EmployeDocument doc = new EmployeDocument();
        doc.setId(employe.getId().getValue());
        doc.setNom(employe.getNom());
        doc.setEmail(employe.getEmail());
        doc.setPoste(employe.getPoste());
        doc.setDateEmbauche(employe.getDateEmbauche());
        doc.setUserId(employe.getUserId());
        return doc;
    }
}
