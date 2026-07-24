package com.pf4erp.todo.adapters.out.persistence.mongo;

import com.pf4erp.todo.application.port.ProjectRepository;
import com.pf4erp.todo.domain.Project;
import com.pf4erp.todo.domain.ProjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

public class MongoProjectRepository implements ProjectRepository {

    private final MongoTemplate mongoTemplate;

    public MongoProjectRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<Project> findById(ProjectId id) {
        return Optional.ofNullable(mongoTemplate.findById(id.getValue(), ProjectDocument.class)).map(this::toDomain);
    }

    @Override
    public List<Project> findAll() {
        return mongoTemplate.findAll(ProjectDocument.class).stream().map(this::toDomain).toList();
    }

    @Override
    public Project save(Project project) {
        ProjectDocument doc = toDocument(project);
        mongoTemplate.save(doc);
        return toDomain(doc);
    }

    @Override
    public void delete(ProjectId id) {
        mongoTemplate.remove(Query.query(Criteria.where("_id").is(id.getValue())), ProjectDocument.class);
    }

    private Project toDomain(ProjectDocument doc) {
        return new Project(
                ProjectId.of(doc.getId()),
                doc.getName(),
                doc.getDescription(),
                doc.getCreatedByUserId(),
                doc.getCreatedAt()
        );
    }

    private ProjectDocument toDocument(Project project) {
        ProjectDocument doc = new ProjectDocument();
        doc.setId(project.getId().getValue());
        doc.setName(project.getName());
        doc.setDescription(project.getDescription());
        doc.setCreatedByUserId(project.getCreatedByUserId());
        doc.setCreatedAt(project.getCreatedAt());
        return doc;
    }
}
