package dev.ctrlspace.genai2506.genaibe.repositories;

import dev.ctrlspace.genai2506.genaibe.models.entities.Document;
import dev.ctrlspace.genai2506.genaibe.models.entities.DocumentSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}
