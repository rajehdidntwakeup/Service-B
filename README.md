# Order Service – Webshop Backend

[![Java](https://img.shields.io/badge/Java-24-blue.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)](https://spring.io/projects/spring-boot)

## Project Overview

The **Order Service** is a core backend component of a webshop system.  
It manages and provides details for all orders made, including:

### Order Details
- **OrderId** - Unique identifier for each order
- **TotalPrice** - Total price of the order
- **Status** - Current order status
- **CreationDate** - Date and time when the order was created
- **OrderItems** - List of products ordered with their respective quantities

### OrderItem Details
- **ID** – Unique identifier for each product
- **ItemId** – Unique identifier for each product in the order
- **ItemName** - Name of the product ordered
- **Quantity** – Quantity of the product ordered
- **Price** – Unit price of the product

The service is built with **Java 24** and **Maven**, following a modular, RESTful design.  
It can be integrated into a larger microservice-based webshop architecture or used as a standalone component.

---

## Usage

### Prerequisites
- Java 24 or later
- Maven 3.9+


### Running the Application

1. Clone the repository:
    ```bash
    git clone https://github.com/fhburgenland-bswe/swm2-ws2025-group-a-order.git

2. Build the Project:
    ```bash
    mvn clean package
3. Run generated JAR file:
    ```bash
   java -jar target/order-service-0.0.1-SNAPSHOT.jar

4. Access the API:
   http://localhost:8081/api/order

## LICENSE

This repository is licensed under the MIT License. See the [LICENSE](LICENSE) file for more information.
Only the specification files are licensed under the MIT License and not the services or their code!