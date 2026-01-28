package dev.genai.genaibe.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = Integer.MAX_VALUE)
    private String email;

    @Column(name = "display_name", nullable = false, length = Integer.MAX_VALUE)
    private String displayName;

    @Column(name = "avatar_url", length = Integer.MAX_VALUE)
    private String avatarUrl;

//    @ColumnDefault("'USER'")
    @Column(name = "role", nullable = false)
    private String role;

    @Column(columnDefinition = "TEXT")
    private String cv_text;
}