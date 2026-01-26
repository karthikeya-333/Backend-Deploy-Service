package com.servce.BackendDeployService.DTO;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class DeployResponse{
    String deploymentId;
    String status;
    String accessUrl;
    String token;
    String message;
}
