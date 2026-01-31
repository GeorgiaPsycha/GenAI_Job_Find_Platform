package dev.genai.genaibe.repositories;

import dev.genai.genaibe.models.entities.Document;
import dev.genai.genaibe.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByCreatedBy(User user);
}
