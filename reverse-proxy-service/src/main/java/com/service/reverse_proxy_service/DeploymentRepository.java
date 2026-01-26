package com.service.reverse_proxy_service;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface DeploymentRepository extends R2dbcRepository<Deployment, Long> {
    Mono<Deployment> findBySubDomain(String subdomain);
}