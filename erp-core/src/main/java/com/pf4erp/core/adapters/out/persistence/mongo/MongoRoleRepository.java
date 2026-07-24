package com.pf4erp.core.adapters.out.persistence.mongo;

import com.pf4erp.core.application.port.out.RoleRepository;
import com.pf4erp.core.domain.auth.Role;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

@Repository
public class MongoRoleRepository implements RoleRepository {

    private final MongoTemplate mongoTemplate;

    public MongoRoleRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, RoleDocument.class)).map(this::toDomain);
    }

    @Override
    public Optional<Role> findByCode(String code) {
        Query query = Query.query(Criteria.where("code").is(code));
        return Optional.ofNullable(mongoTemplate.findOne(query, RoleDocument.class)).map(this::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return mongoTemplate.findAll(RoleDocument.class).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Role> findByCodes(Iterable<String> codes) {
        List<String> list = StreamSupport.stream(codes.spliterator(), false).toList();
        if (list.isEmpty()) {
            return List.of();
        }
        Query query = Query.query(Criteria.where("code").in(list));
        return mongoTemplate.find(query, RoleDocument.class).stream().map(this::toDomain).toList();
    }

    @Override
    public Role save(Role role) {
        RoleDocument doc = toDocument(role);
        mongoTemplate.save(doc);
        return toDomain(doc);
    }

    @Override
    public void deleteById(String id) {
        Query query = Query.query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, RoleDocument.class);
    }

    @Override
    public boolean existsByCode(String code) {
        Query query = Query.query(Criteria.where("code").is(code));
        return mongoTemplate.exists(query, RoleDocument.class);
    }

    @Override
    public long count() {
        return mongoTemplate.count(new Query(), RoleDocument.class);
    }

    private Role toDomain(RoleDocument doc) {
        Set<String> perms = doc.getPermissionCodes() != null
                ? new LinkedHashSet<>(doc.getPermissionCodes())
                : Set.of();
        return new Role(
                doc.getId(),
                doc.getCode(),
                doc.getLabel(),
                doc.getDescription() != null ? doc.getDescription() : "",
                perms,
                doc.isSystem()
        );
    }

    private RoleDocument toDocument(Role role) {
        RoleDocument doc = new RoleDocument();
        doc.setId(role.getId());
        doc.setCode(role.getCode());
        doc.setLabel(role.getLabel());
        doc.setDescription(role.getDescription());
        doc.setPermissionCodes(new LinkedHashSet<>(role.getPermissionCodes()));
        doc.setSystem(role.isSystem());
        return doc;
    }
}
