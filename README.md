# kafka-sidecar
Kafka sidecar for Kubernetes converting Kafka communication into HTTP to simplify business logic implementation

## Requirements

### Java Version
- **Java 21 or higher** (LTS - Long Term Support)
- Recommended: Eclipse Temurin 21, OpenJDK 21, or GraalVM 21
- Required for: Modern Java features (pattern matching, virtual threads, text blocks)

#### Check Your Java Version
```bash
java -version
# Output should show: openjdk 21.x.x or later
```

#### Install Java 21
```bash
# macOS with Homebrew
brew install eclipse-temurin@21

# Or using OpenJDK
brew install openjdk@21

# Verify installation
java -version
```

### Other Requirements
- Maven 3.6+ (included via `./mvnw` wrapper)
- Kubernetes CLI (for K8s targets)
- Helm 3+ (for Helm deployment)

## Building with Java Makefile.java

The project uses a modern Java 21-based build system that runs as a script without compilation.

### Quick Start

```bash
# Show available targets
java Makefile.java help

# Build project (fastest, no tests)
java Makefile.java build

# Build with tests
java Makefile.java build-tests

# Clean artifacts
java Makefile.java clean
```

### Build Targets

| Target        | Description                     |
|---------------|---------------------------------|
| `build`       | Build all modules without tests |
| `build-tests` | Build with full test suite      |
| `clean`       | Clean build artifacts           |
| `test`        | Run unit tests                  |


### Kubernetes/Helm Targets

```bash
java Makefile.java helm-install    # Deploy to K8s
java Makefile.java helm-upgrade    # Update deployment
java Makefile.java helm-status     # Show status
java Makefile.java k8s-status      # Show K8s resources
```

### Optional: Shell Aliases

Add to `~/.zshrc` or `~/.bashrc`:

```bash
alias jbuild='java Makefile.java build'
alias jtest='java Makefile.java test'
alias jdocker='java Makefile.java docker-build'
alias jhelm='java Makefile.java helm-install'
```

Then use: `jbuild`, `jtest`, `jdocker`, `jhelm`

## How It Works

Java 21 allows running `.java` files directly as scripts without compilation:

```bash
# Modern way (Java 21+)
java Makefile.java build

# Traditional way (still works)
javac Makefile.java && java -cp . KafkaSidecarBuild
```

## Troubleshooting

### "java: command not found"
```bash
brew install eclipse-temurin@21
java -version
```

### "Cannot find symbol" error
```bash
# Ensure Java 21 or higher
java -version

# Verify your PATH
which java
```

### Build fails
```bash
# Make mvnw executable
chmod +x ./mvnw

# Clean and rebuild
java Makefile.java clean
java Makefile.java build
```

## Performance

| Operation        | Time          |
|------------------|---------------|
| Build            | 5-10 seconds  |
| Build with tests | 20-30 seconds |
| Docker build     | 2-5 minutes   |
| Helm install     | 30-60 seconds |

## System Architecture

### Complete Data Flow (UML Sequence Diagram)

```mermaid
sequenceDiagram
    participant FakeProducer as fake-producer
    participant Kafka as Kafka Broker
    participant KafkaSidecar as kafka-sidecar
    participant FakeLogic as fake-logic
    participant FakeConsumer as fake-consumer

    Note over FakeProducer,FakeConsumer: 📊 Order Processing Flow (Every 1 second)

    %% Step 1: Produce Orders
    FakeProducer->>FakeProducer: Generate Fake Order
    FakeProducer->>Kafka: Publish to 'orders' topic

    %% Step 2: Consume Orders
    Kafka->>KafkaSidecar: Deliver Order (@KafkaListener)
    KafkaSidecar->>KafkaSidecar: Validate & Store Order

    %% Step 3: Forward Order via HTTP
    KafkaSidecar->>FakeLogic: POST /api/orders (RestTemplate)
    Note over FakeLogic: Order Reception

    %% Step 4: Create Shipment
    FakeLogic->>FakeLogic: Create Shipment<br/>(from Order)
    FakeLogic->>FakeLogic: Set Carrier & Tracking

    %% Step 5: Forward Shipment via HTTP
    FakeLogic->>FakeConsumer: POST /api/shipments (RestTemplate)
    FakeConsumer->>FakeConsumer: Validate Shipment
    FakeConsumer->>FakeLogic: 202 Accepted Response

    %% Step 6: Publish Shipment to Kafka
    FakeConsumer->>Kafka: Publish to 'shipments' topic
    Note over Kafka: HTTP → Kafka Bridge

    %% Step 7: Consume Shipments
    Kafka->>KafkaSidecar: Deliver Shipment (@KafkaListener)
    KafkaSidecar->>KafkaSidecar: Validate & Process Shipment<br/>(Update Status to IN_TRANSIT)

    Note over FakeProducer,FakeConsumer: ✅ Complete cycle ready for next iteration
```

### Component Interactions

| Phase | Source | Destination | Protocol | Data Format | Purpose |
|-------|--------|-------------|----------|-------------|---------|
| 1 | fake-producer | Kafka | KafkaTemplate | JSON Order | Produce orders every 1s |
| 2 | Kafka | kafka-sidecar | @KafkaListener | JSON Order | Consume & validate orders |
| 3 | kafka-sidecar | fake-logic | HTTP POST | JSON Order | Forward to business logic |
| 4 | fake-logic | fake-logic | In-Process | Object | Transform Order → Shipment |
| 5 | fake-logic | fake-consumer | HTTP POST | JSON Shipment | Send for fulfillment |
| 6 | fake-consumer | Kafka | KafkaTemplate | JSON Shipment | Bridge HTTP → Kafka |
| 7 | Kafka | kafka-sidecar | @KafkaListener | JSON Shipment | Track shipments |

### Kafka Topics

| Topic | Producer | Consumer | Partitions | Replication | Purpose |
|-------|----------|----------|-----------|-------------|---------|
| `orders` | fake-producer | kafka-sidecar | 3 | 1 | Order distribution & load balancing |
| `shipments` | fake-consumer | kafka-sidecar | 1 | 1 | Shipment tracking & auditing |

### Consumer Groups

| Group | Service | Topics | Purpose | Offset Strategy |
|-------|---------|--------|---------|-----------------|
| `kafka-sidecar-group` | kafka-sidecar | orders | Order consumption | earliest |
| `kafka-sidecar-shipment-group` | kafka-sidecar | shipments | Shipment consumption | earliest |
| `fake-consumer-shipment-group` | fake-consumer | shipments | Shipment processing | earliest |

### Service Communication Matrix

```
fake-producer ──Kafka──> kafka-sidecar
                         │
                         ├──HTTP POST──> fake-logic
                                         │
                                         ├──HTTP POST──> fake-consumer
                                                         │
                                                         └──Kafka──> kafka-sidecar
```
