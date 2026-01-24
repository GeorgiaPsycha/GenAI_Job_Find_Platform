package dev.genai.genaibe.repositories;

import dev.genai.genaibe.models.entities.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    // Βρες όλες τις αιτήσεις για μια συγκεκριμένη αγγελία (για τον Admin)
    List<Application> findByJobId(UUID jobId);

    // Έλεγχος αν ο χρήστης έχει κάνει ήδη αίτηση σε αυτή την αγγελία
    Optional<Application> findByUserIdAndJobId(UUID userId, UUID jobId);
}
