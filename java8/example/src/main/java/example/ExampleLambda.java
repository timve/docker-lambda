package example;

import com.amazonaws.services.lambda.runtime.Context;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Map;

import static java.util.Arrays.asList;

public class ExampleLambda {

    public void handler(Map<String, String> event, Context context) throws IOException, InterruptedException {
        if (event.containsKey("cmd")) {

            Process cmdProcess = new ProcessBuilder().command(asList("sh", "-c", event.get("cmd"))).start();
            InputStream errorStream = cmdProcess.getErrorStream();
            InputStream inputStream = cmdProcess.getInputStream();

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = errorStream.read(buffer)) != -1) {
                System.err.write(buffer, 0, bytesRead);
            }

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                System.out.write(buffer, 0, bytesRead);
            }
        }

        System.out.println(System.getProperty("java.home"));
        System.out.println(ManagementFactory.getRuntimeMXBean().getInputArguments());
        System.out.println(" -classpath " + System.getProperty("java.class.path"));
        System.out.println(" " + System.getProperty("sun.java.command"));
        System.out.println(System.getProperty("user.dir"));
        System.out.println(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
        System.out.println(System.getenv());
        System.out.println(String.format("cognitoIdentityId: %s", context.getIdentity().getIdentityId()));
        System.out.println(String.format("cognitoIdentityPoolId: %s", context.getIdentity().getIdentityPoolId()));
        System.out.println(String.format("awsRequestId: %s", context.getAwsRequestId()));
        System.out.println(String.format("functionName: %s", context.getFunctionName()));
        System.out.println(String.format("functionVersion: %s", context.getFunctionVersion()));
        System.out.println(String.format("invokedFunctionArn: %s", context.getInvokedFunctionArn()));
        System.out.println(String.format("logGroupName: %s", context.getLogGroupName()));
        System.out.println(String.format("logStreamName: %s", context.getLogStreamName()));
        System.out.println(String.format("memoryLimitInMB: %s", context.getMemoryLimitInMB()));
        System.out.println(String.format("remainingTimeInMillis: %s", context.getRemainingTimeInMillis()));

    }

}