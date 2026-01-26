package com.service.reverse_proxy_service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ReverseProxyController {

    private final DeploymentRepository repository;
    private final WebClient webClient = WebClient.builder().build();

    @Value("${app.domain.base}")
    private String baseDomain;

    @Value("${app.proxy.target-port-node}")
    private int targetPortNode;

    @Value("${app.proxy.target-port-springboot}")
    private int targetPortSpringBoot;

    @RequestMapping("/**")
    public Mono<ResponseEntity<byte[]>> proxy(ServerHttpRequest request) {
        String host = request.getHeaders().getFirst("Host");

        if (host == null || !host.contains(".")) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        String subdomain = host.split("\\.")[0];

        return repository.findBySubDomain(subdomain)
                .flatMap(deployment -> {
                    int port = deployment.getTechStack().equalsIgnoreCase("NODEJS") ? targetPortNode : targetPortSpringBoot;
                    String targetUrl = String.format("http://%s:%d%s",
                            deployment.getPublicIp(),
                            port,
                            request.getPath().value());

                    log.info("Proxying {} -> {}", host, targetUrl);
                    return webClient.method(request.getMethod())
                            .uri(targetUrl)
                            .headers(headers -> headers.addAll(request.getHeaders()))
                            .body(request.getBody(), byte[].class)
                            .retrieve()
                            .toEntity(byte[].class);
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }
}