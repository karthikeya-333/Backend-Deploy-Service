# Backend Deploy Service (BDS)

### What it does
BDS is a deployment orchestrator. It exposes an API that takes a **Git URL**, builds the code into a container on AWS ECS, and provides a unique URL to access the app. It also handles real-time log streaming.

---

### Architecture

<img width="614" height="290" alt="image" src="https://github.com/user-attachments/assets/0e6a126b-0f01-417b-9192-449c5f213237" />


* **Deploy Service:** The core API. It receives the Git URL, triggers ECS tasks, saves deployment data to Supabase, gives access url, and streams logs via WebSockets.
* **Reverse-Proxy-Service:** It maps incoming traffic from custom subdomains to the correct container IP and port.
* **Build-Server-Node:** Contains a Dockerfile and shell scripts used to clone the Git repository, build the Node.js application, and run it inside an ECS task.
* **Build-Server-Spring Boot:** Contains a Dockerfile and shell scripts used to clone the Git repository, build the Spring Boot application, and run it inside an ECS task.


---

### Tech Stack
* **Java / Spring Boot**
* **AWS** (ECS, ECR, Fargate, CloudWatch)
* **Supabase** (PostgreSQL)
* **Docker**

---

### How to Run

#### 1. Push Images to ECR
Build Docker images using the Dockerfile and shell scripts in these folders and push them to your Amazon ECR:
* `build-server-node` – builds and runs Node.js applications
* `build-server-springboot` – builds and runs Spring Boot applications


#### 2. Create ECS Task Definitions
In the AWS Console, create **Fargate** Task Definitions for both runners:
1. **Node Task:** Use the `node-runner` image.
2. **Spring Boot Task:** Use the `springboot-runner` image.
   
*Enable CloudWatch logging so the Deploy Service can tail the logs.*

#### 3. Run the Backend Service
1. Update `application.yml` files in `deploy-servvice` and `reverse-proxy-service` with your AWS credentials, Supabase URL, and ECS Cluster name.
2. Run the applications

```bash
cd deploy-service
./mvnw spring-boot:run

cd reverse-proxy-service
./mvnw spring-boot:run
