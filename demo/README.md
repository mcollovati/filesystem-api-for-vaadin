# Filesystem API Demo

Showcase application for the Filesystem API for Vaadin add-on.

## Build

```bash
mvn package -Pdemo -pl demo -am -DskipTests
```

## Run locally

```bash
java -jar demo/target/filesystem-api-demo-1.0-SNAPSHOT.jar
```

The demo starts on port 8080 by default. Set the `PORT` environment variable to change it.

## Docker

```bash
docker build -t filesystem-api-demo .
docker run -p 8080:8080 filesystem-api-demo
```

## Deploy

Use the `deploy-demo.yml` GitHub Actions workflow dispatch.
