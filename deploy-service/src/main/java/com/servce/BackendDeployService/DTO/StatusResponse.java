package com.servce.BackendDeployService.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusResponse {

    private String accessUrl;

    private String status;

}
