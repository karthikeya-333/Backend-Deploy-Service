package com.servce.BackendDeployService;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    Optional<Deployment> findByToken(String token);

    Optional<Deployment> findByTaskArn(String taskArn);

    Optional<Deployment> findByDeploymentId(String deploymentId);

    boolean existsByAccessUrl(String url);
}