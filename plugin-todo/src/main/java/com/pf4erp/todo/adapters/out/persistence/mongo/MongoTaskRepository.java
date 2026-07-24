package com.pf4erp.todo.adapters.out.persistence.mongo;

import com.pf4erp.todo.application.port.TaskRepository;
import com.pf4erp.todo.domain.ProjectId;
import com.pf4erp.todo.domain.Task;
import com.pf4erp.todo.domain.TaskId;
import com.pf4erp.todo.domain.TaskStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

public class MongoTaskRepository implements TaskRepository {

    private final MongoTemplate mongoTemplate;

    public MongoTaskRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        return Optional.ofNullable(mongoTemplate.findById(id.getValue(), TaskDocument.class)).map(this::toDomain);
    }

    @Override
    public List<Task> findByProjectId(ProjectId projectId) {
        Query query = Query.query(Criteria.where("projectId").is(projectId.getValue()));
        return mongoTemplate.find(query, TaskDocument.class).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Task> findByAssigneeEmployeId(String employeId) {
        Query query = Query.query(Criteria.where("assigneeEmployeId").is(employeId));
        return mongoTemplate.find(query, TaskDocument.class).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Task> findAll() {
        return mongoTemplate.findAll(TaskDocument.class).stream().map(this::toDomain).toList();
    }

    @Override
    public Task save(Task task) {
        TaskDocument doc = toDocument(task);
        mongoTemplate.save(doc);
        return toDomain(doc);
    }

    @Override
    public void delete(TaskId id) {
        mongoTemplate.remove(Query.query(Criteria.where("_id").is(id.getValue())), TaskDocument.class);
    }

    private Task toDomain(TaskDocument doc) {
        return new Task(
                TaskId.of(doc.getId()),
                ProjectId.of(doc.getProjectId()),
                doc.getTitle(),
                doc.getDescription(),
                TaskStatus.from(doc.getStatus()),
                doc.getAssigneeEmployeId(),
                doc.getCreatedByUserId(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }

    private TaskDocument toDocument(Task task) {
        TaskDocument doc = new TaskDocument();
        doc.setId(task.getId().getValue());
        doc.setProjectId(task.getProjectId().getValue());
        doc.setTitle(task.getTitle());
        doc.setDescription(task.getDescription());
        doc.setStatus(task.getStatus().name());
        doc.setAssigneeEmployeId(task.getAssigneeEmployeId());
        doc.setCreatedByUserId(task.getCreatedByUserId());
        doc.setCreatedAt(task.getCreatedAt());
        doc.setUpdatedAt(task.getUpdatedAt());
        return doc;
    }
}
