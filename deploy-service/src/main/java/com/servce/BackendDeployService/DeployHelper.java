package com.servce.BackendDeployService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DeployHelper {

    private final DeploymentRepository deploymentRepository;

    private final List<String> adjectives = Arrays.asList("swift", "vibrant", "azure", "mighty", "fluffy", "silent", "brave");
    private final List<String> nouns = Arrays.asList("panda", "falcon", "nebula", "wave", "shield", "tiger", "comet");
    private final Random random = new Random();

    public String getSubDomainName() {
        String slug;
        boolean exists;
        int attempts = 0;

        do {
            String adj = adjectives.get(random.nextInt(adjectives.size()));
            String noun = nouns.get(random.nextInt(nouns.size()));
            int number = random.nextInt(900) + 100;
            slug = String.format("%s-%s-%d", adj, noun, number);

            exists = deploymentRepository.existsByAccessUrl(slug+".mydomain.com");
            attempts++;

            if (attempts > 10) {
                slug = slug + "-" + UUID.randomUUID().toString().substring(0, 4);
            }
        } while (exists);

        return slug;
    }

    public String generateAccessToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}