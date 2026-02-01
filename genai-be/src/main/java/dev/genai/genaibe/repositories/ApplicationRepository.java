package dev.genai.genaibe.repositories;

import dev.genai.genaibe.models.entities.Application;
import dev.genai.genaibe.models.entities.Document;
import dev.genai.genaibe.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface  ApplicationRepository extends JpaRepository<Application, UUID> {

    // Admin : Find all the applicants for the specific job
    List<Application> findByJobId(UUID jobId);
    boolean existsByJobAndUser(Document job, User user);
    List<Application> findByUser(User user);

    // Check if the User has already applied to this jobId
    Optional<Application> findByUserIdAndJobId(UUID userId, UUID jobId);

    List<Application> findByJob(Document job);
}
