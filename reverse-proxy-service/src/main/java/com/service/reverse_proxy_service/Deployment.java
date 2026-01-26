package com.service.reverse_proxy_service;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "deployments")
@Data
public class Deployment {

    @Id
    private Long id;

    @Column("deployment_id")
    private String deploymentId;

    @Column("sub_domain")
    private String subDomain;

    @Column("access_url")
    private String accessUrl;

    @Column("public_ip")
    private String publicIp;

    @Column("tech_stack")
    private String techStack;

    @Column("status")
    private String status;
}