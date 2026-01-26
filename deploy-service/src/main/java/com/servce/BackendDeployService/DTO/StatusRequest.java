package com.servce.BackendDeployService.DTO;


import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class StatusRequest {
    @NotEmpty
    String deploymentId;

    @NotEmpty
    String token;
}
