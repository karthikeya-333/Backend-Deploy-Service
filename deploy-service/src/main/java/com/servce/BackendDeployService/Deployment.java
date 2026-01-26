package com.servce.BackendDeployService;


import com.servce.BackendDeployService.DTO.TechStack;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deployments")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String deploymentId;

    @Column(nullable = false)
    private String gitUrl;

    @Column(nullable = false, unique = true)
    private String subDomain;

    @Column(nullable = false,unique = true)
    private String accessUrl;

    @Enumerated(EnumType.STRING)
    private TechStack techStack;

    @Column(unique = true, nullable = false)
    private String token;

    private String publicIp;

    private String taskArn;

    private String status;

    private LocalDateTime createdAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }
}