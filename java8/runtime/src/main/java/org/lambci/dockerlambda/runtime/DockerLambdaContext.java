package org.lambci.dockerlambda.runtime;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DockerLambdaContext implements com.amazonaws.services.lambda.runtime.Context {
    private UUID invokeId;
    private String arn;
    private int memoryLimitInMB;
    private String logGroupName;
    private String logStreamName;
    private String functionName;
    private String functionVersion;

    DockerLambdaContext() {
        Map<String, String> environment = System.getenv();
        this.invokeId = randomInvokeId();
        this.arn = arn(
                environment.getOrDefault("AWS_REGION", "us-east-1"),
                environment.getOrDefault("AWS_ACCOUNT_ID", String.valueOf(randomAccountId())),
                environment.getOrDefault("AWS_LAMBDA_FUNCTION_NAME", "test"));
        this.memoryLimitInMB = Integer.parseInt(System.getenv().get("AWS_LAMBDA_FUNCTION_MEMORY_SIZE"));
        this.logGroupName = System.getenv().get("AWS_LAMBDA_LOG_GROUP_NAME");
        this.logStreamName = System.getenv().get("AWS_LAMBDA_LOG_STREAM_NAME");
        this.functionName = System.getenv().get("AWS_LAMBDA_FUNCTION_NAME");
        this.functionVersion = System.getenv().get("AWS_LAMBDA_FUNCTION_VERSION");
    }

    @Override
    public String getAwsRequestId() {
        return invokeId.toString();
    }

    @Override
    public String getLogGroupName() {
        return logGroupName;
    }

    @Override
    public String getLogStreamName() {
        return logStreamName;
    }

    @Override
    public String getFunctionName() {
        return functionName;
    }

    @Override
    public String getFunctionVersion() {
        return functionVersion;
    }

    @Override
    public String getInvokedFunctionArn() {
        return arn;
    }

    @Override
    public CognitoIdentity getIdentity() {
        return new CognitoIdentity() {
            @Override
            public String getIdentityId() {
                return null;
            }

            @Override
            public String getIdentityPoolId() {
                return null;
            }
        };
    }

    @Override
    public ClientContext getClientContext() {
        return null;
    }

    @Override
    public int getRemainingTimeInMillis() {
        return 0;
    }

    @Override
    public int getMemoryLimitInMB() {
        return memoryLimitInMB;
    }

    @Override
    public LambdaLogger getLogger() {
        return System.out::println;
    }

    private static long randomAccountId() {
        return 100000000000L + (long)new Random().nextDouble() * (999999999999L - 100000000000L);
    }

    private static UUID randomInvokeId() {
        return UUID.randomUUID();
    }

    private static String arn(String region, String accountId, String functionName) {
        return String.format("arn:aws:lambda:%s:%s:function:%s", region, accountId, functionName);
    }
}
