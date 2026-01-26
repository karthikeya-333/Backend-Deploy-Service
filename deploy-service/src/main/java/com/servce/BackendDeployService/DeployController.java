package com.servce.BackendDeployService;

import com.servce.BackendDeployService.DTO.DeployRequest;
import com.servce.BackendDeployService.DTO.DeployResponse;
import com.servce.BackendDeployService.DTO.StatusRequest;
import com.servce.BackendDeployService.DTO.StatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeployController {

    private final DeployService deploymentService;

    @PostMapping("/deploy")
    public ResponseEntity<DeployResponse> deploy(@Valid @RequestBody DeployRequest request) {

        return ResponseEntity.ok(deploymentService.initiateDeployment(request.getUrl(),request.getTechStack()));

    }

    @PostMapping("/status")
    public ResponseEntity<StatusResponse> deploy(@Valid @RequestBody StatusRequest request) {

        return ResponseEntity.ok(deploymentService.getStatus(request.getDeploymentId(),request.getToken()));

    }




}