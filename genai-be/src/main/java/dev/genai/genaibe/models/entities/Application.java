package dev.genai.genaibe.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "job_id"}) // Ένας χρήστης, μία αίτηση ανά αγγελία
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    // Ποιος έκανε την αίτηση
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Για ποια αγγελία (Document)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "job_id", nullable = false) // Προσοχή: Στη βάση λέγεται job_id, αλλά δείχνει στο document
    private Document job;

    @Column(name = "cv_file_url", length = Integer.MAX_VALUE)
    private String cvFileUrl;

    @Column(name = "cv_content_text", length = Integer.MAX_VALUE)
    private String cvContentText;

    @Column(name = "motivation_text", length = Integer.MAX_VALUE)
    private String motivationText;

    @ColumnDefault("'APPLIED'")
    @Column(name = "status", nullable = false)
    private String status; // APPLIED, REVIEWED, REJECTED, HIRED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}