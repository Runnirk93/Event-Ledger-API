# Event Ledger API

A robust Spring Boot REST API tracking financial transactions while dealing with distributed systems anomalies like out-of-order deliveries and simultaneous duplicate requests.

## How to Execute the Project Stack

* **Execute Tests via Maven**:
  ```bash
  mvn test
  ```
* **Run App Locally via Maven**:
  ```bash
  mvn spring-boot:run
  ```
* **Run App via Docker Container**:
  ```bash
  docker-compose up --build -d
  ```

## API Documentation Links
* **Interactive Swagger UI Dashboard**: http://localhost:8080/swagger-ui.html
* **Embedded H2 SQL Console**: http://localhost:8080/h2-console
