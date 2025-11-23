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

## Code Style: Checkstyle

This project uses Checkstyle to enforce a consistent Java coding style.

- Configuration: config/checkstyle/checkstyle.xml (with suppressions in config/checkstyle/checkstyle-suppressions.xml)
- Maven plugin: org.apache.maven.plugins:maven-checkstyle-plugin (configured in pom.xml)
- Fails the build on violations (default behavior in this repo)

How to run locally:

- Quick check: mvn -B checkstyle:check
- Full validation: mvn -B clean verify

``` bash
mvn checkstyle:check
```

Reports:

- HTML report: target/reports/checkstyle.html
- XML report: target/checkstyle-result.xml

Continuous Integration:

- Checkstyle runs in CI for every pull request. See .github/workflows/verify.yml
- Checkstyle violations fail the pipeline.

## Code Style: PMD

This project integrates PMD (Programming Mistake Detector)
to identify common programming issues and enforce best practices automatically during continuous integration.

- Configuration: config/pmd/ruleset.xml (customized ruleset for this project)
- Maven plugin: org.apache.maven.plugins:maven-pmd-plugin (configured in pom.xml)
- Fails the build on rule violations — no code with PMD errors can be merged.

How to run locally:

- Quick check: mvn -B pmd:check
- Full validation: mvn -B clean verify

``` bash
mvn pmd:check
```

Reports:

- XML report: target/pmd.xml

Continuous Integration:

- PMD runs automatically in GitHub Actions as part of the verify.yml workflow.
- Uses Java 24 (Amazon Corretto) runtime.
- Runs before the build job — if PMD fails, the build is skipped.
- Ensures consistent code quality and prevents merging of code with PMD violations.

## SpotBugs Integration in CI/CD

SpotBugs is integrated into the GitHub Actions CI/CD workflow to ensure all code meets quality and reliability standards
before being merged into main.

Workflow Details

The SpotBugs job runs automatically as part of the pipeline defined in .github/workflows/verify.yml.

It uses Amazon Corretto 24 to analyze the Java source code for potential bugs and bad practices.

The build job depends on the successful completion of the SpotBugs analysis — if SpotBugs fails, the pipeline will not
continue.

Running SpotBugs Locally

You can also run SpotBugs manually to analyze your code before committing changes:

```bash
mvn spotbugs:check
```

### Clean and compile the project

```bash
mvn clean compile
```

### Run SpotBugs analysis

mvn spotbugs:spotbugs

Reports are generated in:

target/spotbugsXml.xml — XML report format

## Dockerfile Linting with Hadolint

This project includes a Hadolint job in the CI/CD pipeline to ensure that the Dockerfile follows best practices and
coding standards.

### Workflow Details:

1. **Checks if a `Dockerfile` exists**:
    - Skips Hadolint if absent.

2. **Hadolint Execution**:
    - Uses the `hadolint/hadolint-action@v3.1.0` and scans the `Dockerfile`.
    - Ignores certain linting rules (e.g., `DL3008`, `DL3013`).

3. **Manual Installation**:
    - Hadolint is installed directly on the runner for additional checks:
      ```bash
      wget -O /usr/local/bin/hadolint https://github.com/hadolint/hadolint/releases/latest/download/hadolint-Linux-x86_64
      chmod +x /usr/local/bin/hadolint
      ```

4. **Failure Handling**:
   Warnings are ignored, but errors cause the pipeline to fail:
    ```bash
    hadolint Dockerfile --failure-threshold error
    ```

### Run Locally:

If you want to verify your `Dockerfile` locally with Hadolint:

1. Install Hadolint:
    ```bash
    wget -O /usr/local/bin/hadolint https://github.com/hadolint/hadolint/releases/latest/download/hadolint-Linux-x86_64
    chmod +x /usr/local/bin/hadolint
    ```
2. Run linting:
    ```bash
    hadolint Dockerfile
    ```

Hadolint ensures that Docker images are built following best practices to optimize size, security, and build speed.

## LICENSE

This repository is licensed under the MIT License. See the [LICENSE](LICENSE) file for more information.
Only the specification files are licensed under the MIT License and not the services or their code!