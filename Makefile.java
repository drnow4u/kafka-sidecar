import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Kafka Sidecar Build System
 * Run: java Makefile.java [target]
 * Example: java Makefile.java build
 */
void main(String[] args) {
    var build = new KafkaSidecarBuild();
    String target = args.length > 0 ? args[0] : "help";
    
    try {
        build.execute(target);
    } catch (Exception e) {
        System.err.println("❌ Error: " + e.getMessage());
        e.printStackTrace();
        System.exit(1);
    }
}

class KafkaSidecarBuild {
    private static final String RESET = "\033[0m";
    private static final String BLUE = "\033[0;36m";
    private static final String GREEN = "\033[0;32m";
    private static final String YELLOW = "\033[0;33m";
    private static final String RED = "\033[0;31m";
    
    private static final String PROJECT_NAME = "kafka-sidecar";
    private static final String DOCKER_REGISTRY = "kafka-sidecar";
    private static final String HELM_RELEASE = "kafka-sidecar";
    private static final String HELM_NAMESPACE = "kafka-sidecar";
    private static final String K8S_NAMESPACE = "kafka-sidecar";
    
    private final Map<String, Runnable> targets = new LinkedHashMap<>();
    
    KafkaSidecarBuild() {
        initializeTargets();
    }
    
    private void initializeTargets() {
        targets.put("help", this::help);
        targets.put("build", this::build);
        targets.put("build-tests", this::buildTests);
        targets.put("clean", this::clean);
        targets.put("test", this::test);
        targets.put("docker-build", this::dockerBuild);
        targets.put("helm-install", this::helmInstall);
        targets.put("helm-upgrade", this::helmUpgrade);
        targets.put("helm-uninstall", this::helmUninstall);
        targets.put("helm-status", this::helmStatus);
        targets.put("k8s-status", this::k8sStatus);
        targets.put("test-api", this::testAPI);
    }
    
    void execute(String target) throws Exception {
        Runnable runnable = targets.get(target);
        if (runnable == null) {
            println(RED + "❌ Unknown target: " + target + RESET);
            help();
            System.exit(1);
        }
        runnable.run();
    }
    
    // ==================== HELP ====================
    private void help() {
        println(BLUE + "Kafka Sidecar Build System (Java 25)" + RESET);
        println(YELLOW + "\nAvailable targets:" + RESET);
        println("");
        
        targets.forEach((name, _) -> 
            printf("  %-25s %s%n", GREEN + name + RESET, getDescription(name))
        );
        
        println("\n" + YELLOW + "Usage: java Makefile.java [target]" + RESET);
        println(YELLOW + "Example: java Makefile.java build" + RESET);
    }
    
    private String getDescription(String target) {
        return switch (target) {
            case "help" -> "Show this help message";
            case "build" -> "Build all modules";
            case "build-tests" -> "Build with tests";
            case "clean" -> "Clean build artifacts";
            case "test" -> "Run all tests";
            case "docker-build" -> "Build Docker images";
            case "helm-install" -> "Install Helm chart";
            case "helm-upgrade" -> "Upgrade Helm chart";
            case "helm-uninstall" -> "Uninstall Helm chart";
            case "helm-status" -> "Show Helm status";
            case "k8s-status" -> "Show Kubernetes status";
            case "test-api" -> "Test REST APIs";
            default -> "";
        };
    }
    
    // ==================== BUILD ====================
    private void build() {
        println(BLUE + "Building all modules..." + RESET);
        runCommand("./mvnw", "clean", "package", "-DskipTests");
        println(GREEN + "✓ Build complete" + RESET);
    }
    
    private void buildTests() {
        println(BLUE + "Building with tests..." + RESET);
        runCommand("./mvnw", "clean", "package");
        println(GREEN + "✓ Build with tests complete" + RESET);
    }
    
    private void clean() {
        println(BLUE + "Cleaning build artifacts..." + RESET);
        runCommand("./mvnw", "clean");
        println(GREEN + "✓ Clean complete" + RESET);
    }
    
    // ==================== TESTS ====================
    private void test() {
        println(BLUE + "Running tests..." + RESET);
        runCommand("./mvnw", "test");
        println(GREEN + "✓ Tests complete" + RESET);
    }
    
    // ==================== DOCKER ====================
    private void dockerBuild() {
        println(BLUE + "Building Docker images..." + RESET);
        buildDockerImage("Dockerfile.producer", "fake-producer");
        buildDockerImage("Dockerfile.logic", "fake-logic");
        buildDockerImage("Dockerfile.consumer", "fake-consumer");
        buildDockerImage("Dockerfile.sidecar", "kafka-sidecar");
        println(GREEN + "✓ Docker images built successfully!" + RESET);
    }
    
    private void buildDockerImage(String dockerfile, String imageName) {
        println(BLUE + "  Building " + imageName + "..." + RESET);
        runCommand("docker", "build",
            "-f", dockerfile,
            "-t", "kafka-sidecar/" + imageName + ":latest",
            ".");
        println(GREEN + "  ✓ " + imageName + " image built" + RESET);
    }
    
    // ==================== HELM ====================
    private void helmInstall() {
        println(BLUE + "Installing Helm chart..." + RESET);
        println(YELLOW + "⏳ Using --wait flag to wait for resources to be ready..." + RESET);
        runCommand("helm", "install", HELM_RELEASE, "./helm",
            "--namespace", HELM_NAMESPACE,
            "--create-namespace",
            "--wait",
            "--wait-for-jobs",
            "--timeout", "10m");
        println(GREEN + "✓ Helm chart installed and all resources are ready!" + RESET);
        helmStatus();
    }
    
    private void helmUpgrade() {
        println(BLUE + "Upgrading Helm chart..." + RESET);
        println(YELLOW + "⏳ Using --wait flag to wait for resources to be ready..." + RESET);
        runCommand("helm", "upgrade", HELM_RELEASE, "./helm",
            "--namespace", HELM_NAMESPACE,
            "--wait",
            "--wait-for-jobs",
            "--timeout", "10m");
        println(GREEN + "✓ Helm chart upgraded and all resources are ready!" + RESET);
        helmStatus();
    }
    
    private void helmUninstall() {
        println(BLUE + "Uninstalling Helm chart..." + RESET);
        runCommand("helm", "uninstall", HELM_RELEASE,
            "--namespace", HELM_NAMESPACE);
        println(GREEN + "✓ Helm chart uninstalled" + RESET);
    }
    
    private void helmStatus() {
        println(BLUE + "Helm Release Status:" + RESET);
        runCommand("helm", "status", HELM_RELEASE, "-n", HELM_NAMESPACE);
        println("");
        println(BLUE + "Kubernetes Resources:" + RESET);
        runCommand("kubectl", "get", "all", "-n", HELM_NAMESPACE);
    }
    
    // ==================== KUBERNETES ====================
    private void k8sStatus() {
        println(BLUE + "Kubernetes Status:" + RESET);
        println("\n" + BLUE + "Namespace:" + RESET);
        runCommand("kubectl", "get", "namespace", K8S_NAMESPACE);
        
        println("\n" + BLUE + "Deployments:" + RESET);
        runCommand("kubectl", "get", "deployments", "-n", K8S_NAMESPACE);
        
        println("\n" + BLUE + "Pods:" + RESET);
        runCommand("kubectl", "get", "pods", "-n", K8S_NAMESPACE);
        
        println("\n" + BLUE + "Services:" + RESET);
        runCommand("kubectl", "get", "services", "-n", K8S_NAMESPACE);
        
        println("\n" + BLUE + "HPA (Auto-scaling):" + RESET);
        runCommand("kubectl", "get", "hpa", "-n", K8S_NAMESPACE);
    }
    
    // ==================== API TESTING ====================
    private void testAPI() {
        println(BLUE + "Testing REST APIs..." + RESET);
        println("");
        
        testEndpoint("http://localhost:8080/api/orders/list", "Orders API");
        testEndpoint("http://localhost:8081/api/shipments/health", "Shipments API");
        testEndpoint("http://localhost:8080/actuator/health", "Health Check");
    }
    
    private void testEndpoint(String url, String name) {
        println(YELLOW + "Testing " + name + ": " + url + RESET);
        try {
            runCommand("curl", "-s", url);
            println("");
        } catch (Exception e) {
            println(RED + "  ❌ Endpoint not responding" + RESET);
        }
    }
    
    // ==================== UTILITY METHODS ====================
    private void runCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new RuntimeException("Command failed with exit code " + exitCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error running command: " + String.join(" ", command), e);
        }
    }
    
    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void println(String message) {
        System.out.println(message);
    }
    
    private void printf(String format, Object... args) {
        System.out.printf(format, args);
    }
}