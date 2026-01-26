package com.servce.BackendDeployService.DTO;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeployRequest {

    @NotEmpty
    String url;

    @NotNull
    TechStack techStack;

}


