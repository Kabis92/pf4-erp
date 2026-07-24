package com.pf4erp.core.adapters.out.persistence.mongo;

import com.pf4erp.core.application.port.out.UserAccountRepository;
import com.pf4erp.core.domain.auth.UserAccount;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class MongoUserAccountRepository implements UserAccountRepository {

    private final MongoTemplate mongoTemplate;

    public MongoUserAccountRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<UserAccount> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, UserAccountDocument.class)).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        Query query = Query.query(Criteria.where("username").is(username));
        return Optional.ofNullable(mongoTemplate.findOne(query, UserAccountDocument.class)).map(this::toDomain);
    }

    @Override
    public List<UserAccount> findAll() {
        return mongoTemplate.findAll(UserAccountDocument.class).stream().map(this::toDomain).toList();
    }

    @Override
    public UserAccount save(UserAccount user) {
        UserAccountDocument doc = toDocument(user);
        mongoTemplate.save(doc);
        return toDomain(doc);
    }

    @Override
    public void deleteById(String id) {
        Query query = Query.query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, UserAccountDocument.class);
    }

    @Override
    public boolean existsByUsername(String username) {
        Query query = Query.query(Criteria.where("username").is(username));
        return mongoTemplate.exists(query, UserAccountDocument.class);
    }

    @Override
    public long count() {
        return mongoTemplate.count(new Query(), UserAccountDocument.class);
    }

    private UserAccount toDomain(UserAccountDocument doc) {
        return new UserAccount(
                doc.getId(),
                doc.getUsername(),
                doc.getDisplayName(),
                doc.getPasswordHash(),
                doc.getRoleCodes() != null ? doc.getRoleCodes() : Set.of(),
                doc.isEnabled()
        );
    }

    private UserAccountDocument toDocument(UserAccount user) {
        UserAccountDocument doc = new UserAccountDocument();
        doc.setId(user.getId());
        doc.setUsername(user.getUsername());
        doc.setDisplayName(user.getDisplayName());
        doc.setPasswordHash(user.getPasswordHash());
        doc.setRoleCodes(new LinkedHashSet<>(user.getRoleCodes()));
        doc.setEnabled(user.isEnabled());
        return doc;
    }
}
