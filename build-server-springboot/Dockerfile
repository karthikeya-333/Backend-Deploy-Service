FROM maven:3.9.6-eclipse-temurin-17

RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ENV SERVER_PORT=8080

COPY main.sh /main.sh
RUN chmod +x /main.sh

ENTRYPOINT ["/main.sh"]