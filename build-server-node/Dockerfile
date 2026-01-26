FROM node:20-slim

RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ENV PORT=3000

COPY main.sh /main.sh
RUN chmod +x /main.sh

ENTRYPOINT ["/main.sh"]