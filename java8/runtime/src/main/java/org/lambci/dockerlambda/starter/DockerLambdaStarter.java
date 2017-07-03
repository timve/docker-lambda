package org.lambci.dockerlambda.starter;

import org.lambci.dockerlambda.runtime.DockerLambdaExecutor;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DockerLambdaStarter {

    public static void main(String[] args) throws IOException {
        Map<String, String> env = System.getenv();

        String handlerIdentifier = args.length > 0 ? args[0] : env.getOrDefault("AWS_LAMBDA_FUNCTION_HANDLER", "lambda_function::lambda_handler");
        String body = args.length > 1 ? args[1] : env.getOrDefault("AWS_LAMBDA_EVENT_BODY", "{}");

        int maxMemoryInKB = Integer.parseInt(System.getenv().getOrDefault("AWS_LAMBDA_FUNCTION_MEMORY_SIZE", String.valueOf(1536))) * 1024;
        long maxHeap = Math.round(maxMemoryInKB * 0.85);
        long maxMetaspace = Math.round(maxMemoryInKB * 0.1);
        long reservedCodeCache = maxMemoryInKB - (maxHeap + maxMetaspace);

        ProcessBuilder processBuilder = new ProcessBuilder().inheritIO().command(
                "java",
                "-classpath", "/var/runtime/lib/awslambda.jar:/var/task/*",
                String.format("-XX:MaxHeapSize=%dk", maxHeap), // 85%
                String.format("-XX:MaxMetaspaceSize=%dk", maxMetaspace), // 10%
                String.format("-XX:ReservedCodeCacheSize=%dk", reservedCodeCache), // 5%
                "-XX:+UseSerialGC",
                "-Xshare:on",
                "-XX:-TieredCompilation",
                // TODO figure out remote debugging -> -Xshare:on seems to break this.
//                "-Xdebug",
//                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005",
                DockerLambdaExecutor.class.getName(),
                handlerIdentifier,
                body);

        processBuilder.environment().putAll(createProcessEnvironment(maxMemoryInKB));

        Process process = processBuilder.start();
        // TODO can we do this in a less hacky way ?
        while (process.isAlive()) {
            // And now we wait ...
        }

        System.exit(process.exitValue());
    }

    private static Map<String, String> createProcessEnvironment(int maxMemory) {
        Map<String, String> env = System.getenv();

        LocalDate today = LocalDate.now();
        String region = env.getOrDefault("AWS_REGION", "us-east-1");
        String version = env.getOrDefault("AWS_LAMBDA_FUNCTION_VERSION", "$LATEST");
        String functionName = env.getOrDefault("AWS_LAMBDA_FUNCTION_NAME", "test");

        Map<String, String> environment = new HashMap<>();
        environment.put("AWS_LAMBDA_LOG_GROUP_NAME", String.format("/aws/lambda/%s", functionName));
        environment.put("AWS_LAMBDA_LOG_STREAM_NAME", String.format("%s/%s/%s/[%s]%s",
                today.getYear(),
                today.getMonth(),
                today.getDayOfMonth(),
                version,
                String.format("%016x", (long)Math.pow(16, 16))
        ));
        environment.put("AWS_LAMBDA_FUNCTION_NAME", functionName);
        environment.put("AWS_LAMBDA_FUNCTION_MEMORY_SIZE", String.valueOf(maxMemory / 1024));
        environment.put("AWS_LAMBDA_FUNCTION_VERSION", version);
        environment.put("AWS_REGION", region);
        environment.put("AWS_DEFAULT_REGION", region);

        return environment;
    }
}