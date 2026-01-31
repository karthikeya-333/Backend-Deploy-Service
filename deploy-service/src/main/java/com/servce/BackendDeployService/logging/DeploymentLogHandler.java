package com.servce.BackendDeployService.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servce.BackendDeployService.DTO.TechStack;
import com.servce.BackendDeployService.Deployment;
import com.servce.BackendDeployService.DeploymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsResponse;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeploymentLogHandler extends TextWebSocketHandler {

    private final DeploymentRepository repository;
    private final CloudWatchLogsClient cloudWatchLogsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String accessUrl = json.get("accessUrl").asText();
        String userToken = json.get("token").asText();

        Deployment deployment = repository.findByAccessUrl(accessUrl)
                .orElseThrow(() -> new Exception("No deployment found for: " + accessUrl));

        if (!deployment.getToken().equals(userToken)) {
            session.sendMessage(new TextMessage("Error: Invalid Token"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        startLogStream(session, deployment.getTaskArn(), deployment.getTechStack());
    }

    private void startLogStream(WebSocketSession session, String taskArn, TechStack techStack) {
        new Thread(() -> {
            try {
                String[] arnParts = taskArn.split("/");
                String id = arnParts[arnParts.length - 1];

                String logStreamName = (techStack.equals(TechStack.NODEJS)) ? "ecs/node-runner/" + id : "ecs/springboot-runner/" + id;
                String logGroupName = (techStack.equals(TechStack.NODEJS)) ? "/ecs/node-runner-task" : "/ecs/springboot-runner-task";

                String lastToken = null;

                while (session.isOpen()) {
                    GetLogEventsRequest.Builder requestBuilder = GetLogEventsRequest.builder()
                            .logGroupName(logGroupName)
                            .logStreamName(logStreamName);

                    if (lastToken == null) {
                        requestBuilder.limit(50).startFromHead(false);
                    } else {
                        requestBuilder.nextToken(lastToken);
                    }

                    GetLogEventsResponse response = cloudWatchLogsClient.getLogEvents(requestBuilder.build());

                    if (!response.events().isEmpty()) {
                        for (var event : response.events()) {
                            if (session.isOpen()) {
                                session.sendMessage(new TextMessage(event.message()));
                            }
                        }
                    }

                    lastToken = response.nextForwardToken();
                    Thread.sleep(3000);
                }
            } catch (Exception e) {
                try {
                    log.error("Error streaming logs", e);
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage("Error retrieving logs: " + e.getMessage()));
                        session.close();
                    }
                } catch (IOException ignored) {}
            }
        }).start();
    }
}