package dev.genai.genaibe.repositories;

import dev.genai.genaibe.models.entities.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}
