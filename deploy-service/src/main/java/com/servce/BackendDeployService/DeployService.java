package com.servce.BackendDeployService;

import com.servce.BackendDeployService.DTO.DeployResponse;
import com.servce.BackendDeployService.DTO.StatusResponse;
import com.servce.BackendDeployService.DTO.TechStack;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;
import software.amazon.awssdk.services.ecs.waiters.EcsWaiter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeployService {

    private final DeploymentRepository deploymentRepository;
    private final DeployHelper deployHelper;

    private final Executor deploymentExecutor = Executors.newFixedThreadPool(5);

    @Value("${app.aws.ecs.cluster-name}")
    private String CLUSTER_NAME;

    @Value("${app.aws.ecs.security-group}")
    private String SECURITY_GROUP;

    @Value("${app.aws.ecs.subnet-1}")
    private String SUBNET_1;

    @Value("${app.aws.ecs.subnet-2}")
    private String SUBNET_2;

    @Value("${app.base-domain}")
    private String BASE_DOMAIN;

    @Value("${app.aws.region}")
    private String AWS_REGION;

    public DeployResponse initiateDeployment(String githubUrl, TechStack techStack) {

        String deployId = UUID.randomUUID().toString().substring(0, 8);
        String subDomain = deployHelper.getSubDomainName();
        String finalUrl = subDomain + "." + BASE_DOMAIN;
        String token = deployHelper.generateAccessToken();

        Deployment deployment = Deployment.builder().deploymentId(deployId).subDomain(subDomain).techStack(techStack).gitUrl(githubUrl).status("PENDING").accessUrl(finalUrl).token(token).build();
        deploymentRepository.save(deployment);

        CompletableFuture.runAsync(() -> {
            startDeploymentTask(githubUrl, techStack, subDomain, deployId);
        }, deploymentExecutor);

        return DeployResponse.builder().accessUrl(finalUrl).deploymentId(deployId).token(token).status("PENDING").message("The deployment has started").build();
    }

    @Async
    public void startDeploymentTask(String githubUrl, TechStack techStack, String subdomain, String deployId) {

        try (EcsClient ecsClient = EcsClient.builder()
                .region(Region.of(AWS_REGION))
                .build();
             Ec2Client ec2Client = Ec2Client.builder()
                     .region(Region.of(AWS_REGION))
                     .build()) {

            log.info("Starting deployment for deployId: {} with stack: {}", deployId, techStack);

            String family = (techStack == TechStack.NODEJS) ? "node-runner-task" : "springboot-runner-task";
            String containerName = (techStack == TechStack.NODEJS) ? "node-runner" : "springboot-runner";

            ContainerOverride containerOverride = ContainerOverride.builder()
                    .name(containerName)
                    .environment(KeyValuePair.builder().name("GITHUB_URL").value(githubUrl).build())
                    .build();

            AwsVpcConfiguration vpcConfig = AwsVpcConfiguration.builder()
                    .subnets(SUBNET_1, SUBNET_2)
                    .securityGroups(SECURITY_GROUP)
                    .assignPublicIp(AssignPublicIp.ENABLED)
                    .build();

            RunTaskRequest runTaskRequest = RunTaskRequest.builder()
                    .cluster(CLUSTER_NAME)
                    .taskDefinition(family)
                    .launchType(LaunchType.FARGATE)
                    .networkConfiguration(n -> n.awsvpcConfiguration(vpcConfig))
                    .overrides(o -> o.containerOverrides(containerOverride))
                    .build();

            RunTaskResponse response = ecsClient.runTask(runTaskRequest);
            if (!response.hasTasks()) {
                log.error("Failed to start task for {}. Reason: {}", subdomain, response.failures());
                throw new RuntimeException("No tasks started");
            }

            String taskArn = response.tasks().get(0).taskArn();
            log.info("Task triggered successfully.");

            EcsWaiter waiter = ecsClient.waiter();
            waiter.waitUntilTasksRunning(r -> r.cluster(CLUSTER_NAME).tasks(taskArn));

            DescribeTasksResponse describeResponse = ecsClient.describeTasks(r -> r.cluster(CLUSTER_NAME).tasks(taskArn));

            String eniId = describeResponse.tasks().get(0).attachments().get(0).details().stream()
                    .filter(d -> d.name().equals("networkInterfaceId"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Network Interface not found for task " + taskArn))
                    .value();

            log.debug("Found ENI ID: {} for task {}", eniId, taskArn);

            DescribeNetworkInterfacesResponse eniResponse = ec2Client.describeNetworkInterfaces(
                    DescribeNetworkInterfacesRequest.builder().networkInterfaceIds(eniId).build()
            );

            String publicIp = eniResponse.networkInterfaces().get(0).association().publicIp();

            deploymentRepository.findByDeploymentId(deployId).ifPresent(deployment -> {
                deployment.setStatus("DEPLOYED");
                deployment.setPublicIp(publicIp);
                deployment.setTaskArn(taskArn);
                deploymentRepository.save(deployment);
            });

            log.info("Deployment Success! Subdomain: {} is mapped to Public IP: {}", subdomain, publicIp);


        } catch (Exception e) {
            log.error("Deployment failed for {}: {}", deployId, e.getMessage());
            deploymentRepository.findByDeploymentId(deployId).ifPresent(d -> {
                d.setStatus("FAILED");
                deploymentRepository.save(d);
            });
        }
    }

    public StatusResponse getStatus( String deploymentId, String token) {
        return deploymentRepository.findByDeploymentId(deploymentId)
                .filter(deployment -> deployment.getToken().equals(token))
                .map(deployment -> StatusResponse.builder()
                        .accessUrl(deployment.getAccessUrl())
                        .status(deployment.getStatus())
                        .build())
                .orElseThrow(() -> new RuntimeException("Invalid deployment ID or token"));
    }
}