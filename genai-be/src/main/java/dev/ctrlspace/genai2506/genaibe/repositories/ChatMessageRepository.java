package dev.ctrlspace.genai2506.genaibe.repositories;

import dev.ctrlspace.genai2506.genaibe.models.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
}
