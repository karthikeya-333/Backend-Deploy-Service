package com.servce.BackendDeployService.logging;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DeploymentLogHandler deploymentLogHandler;

    public WebSocketConfig(DeploymentLogHandler deploymentLogHandler) {
        this.deploymentLogHandler = deploymentLogHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(deploymentLogHandler, "/logs")
                .setAllowedOrigins("*");
    }
}