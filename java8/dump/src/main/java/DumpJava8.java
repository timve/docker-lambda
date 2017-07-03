import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Map;

import static java.util.Arrays.asList;

public class DumpJava8 {

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

        String filename = "java8.tgz";
        String cmd = String.format("tar -cpzf /tmp/%s --numeric-owner --ignore-failed-read /var/runtime /var/lang", filename);

        Process zipProcess = new ProcessBuilder().command(asList("sh", "-c", cmd)).start();
        zipProcess.waitFor();

        System.out.println(String.format("Zipping done! Exit code was: %s Uploading...", zipProcess.exitValue()));

        PutObjectRequest request = new PutObjectRequest("lambci", "fs/" + filename, new FileInputStream(String.format("/tmp/%s", filename)), new ObjectMetadata());
        request.setCannedAcl(CannedAccessControlList.PublicRead);
        AmazonS3ClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain()).build().putObject(request);

        System.out.println("Uploading done!");

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

/*
/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.121-0.b13.29.amzn1.x86_64/jre
[-XX:MaxHeapSize=445645k, -XX:MaxMetaspaceSize=52429k, -XX:ReservedCodeCacheSize=26214k, -XX:+UseSerialGC, -Xshare:on, -XX:-TieredCompilation]
 -classpath /var/runtime/lib/LambdaJavaRTEntry-1.0.jar
 /var/runtime/lib/LambdaJavaRTEntry-1.0.jar
/
/var/task/
{
    PATH=/usr/local/bin:/usr/bin/:/bin,
    _AWS_XRAY_DAEMON_ADDRESS=169.254.79.2,
    LAMBDA_TASK_ROOT=/var/task,
    AWS_LAMBDA_FUNCTION_MEMORY_SIZE=512,
    TZ=:UTC,
    AWS_SECRET_ACCESS_KEY=JZvD...BDZ4L,
    AWS_EXECUTION_ENV=AWS_Lambda_java8,
    AWS_DEFAULT_REGION=us-east-1,
    AWS_LAMBDA_LOG_GROUP_NAME=/aws/lambda/dump-java8,
    env1=val1,
    XFILESEARCHPATH=/usr/dt/app-defaults/%L/Dt,
    _HANDLER=DumpJava8::handler,
    LANG=en_US.UTF-8,
    LAMBDA_RUNTIME_DIR=/var/runtime,
    AWS_SESSION_TOKEN=FQoDYXdzEMb//////////...0oog7bzuQU=,
    AWS_ACCESS_KEY_ID=ASIA...C37A,
    LD_LIBRARY_PATH=/lib64:/usr/lib64:/var/runtime:/var/runtime/lib:/var/task:/var/task/lib,
    _X_AMZN_TRACE_ID=Root=1-dc99d00f-c079a84d433534434534ef0d;Parent=91ed514f1e5c03b2;Sampled=0,
    AWS_SECRET_KEY=JZvD...BDZ4L,
    AWS_REGION=us-east-1,
    AWS_LAMBDA_LOG_STREAM_NAME=2017/03/23/[$LATEST]c079a84d433534434534ef0ddc99d00f,
    AWS_XRAY_DAEMON_ADDRESS=169.254.79.2:2000,
    _AWS_XRAY_DAEMON_PORT=2000,
    NLSPATH=/usr/dt/lib/nls/msg/%L/%N.cat,
    AWS_XRAY_CONTEXT_MISSING=LOG_ERROR,
    AWS_LAMBDA_FUNCTION_VERSION=$LATEST,
    AWS_ACCESS_KEY=ASIA...C37A,
    AWS_LAMBDA_FUNCTION_NAME=dump-java8
}
cognitoIdentityId:
cognitoIdentityPoolId:
awsRequestId: 1fcdc383-a9e8-4228-bc1c-8db17629e183
functionName: dump-java8
functionVersion: $LATEST
invokedFunctionArn: arn:aws:lambda:us-east-1:263126357258:function:dump-java8
logGroupName: /aws/lambda/dump-java8
logStreamName: 2017/03/23/[$LATEST]c079a84d433534434534ef0ddc99d00f
memoryLimitInMB: 1536
remainingTimeInMillis: 5524
 */