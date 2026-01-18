package dev.ctrlspace.genai2506.genaibe.repositories;

import dev.ctrlspace.genai2506.genaibe.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
